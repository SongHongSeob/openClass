package com.hongseob.openclass_ap.enrollment.worker;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseStatus;
import com.hongseob.openclass_ap.enrollment.Enrollment;
import com.hongseob.openclass_ap.enrollment.EnrollmentRepository;
import com.hongseob.openclass_ap.enrollment.EnrollmentStatus;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.enrollment.request.RequestType;
import com.hongseob.openclass_ap.waitlist.WaitlistEntry;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import com.hongseob.openclass_ap.waitlist.WaitlistStatus;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 큐 요청 처리 — 클레임 + 요청 1건 처리(design.md §4.1-4.2).
 *
 * <p><b>M2 범위</b>(plan.md §F M2, REQ-WRK-001~015): {@code ENROLL} 요청의
 * 전체 디스패치(마감 분기 · 중복 3종 검사 · 확정/대기 분기)와 실패 격리
 * ({@link #processOne}이 예외로 롤백된 뒤 별도 {@code REQUIRES_NEW}
 * 트랜잭션에서 {@code FAILED}만 기록하는 {@link #recordFailure})를
 * 구현한다.</p>
 *
 * <p><b>M4 범위</b>(plan.md §F M4, REQ-WL-001~011·REQ-CNL-001~005): {@code
 * CANCEL} 요청의 전체 디스패치(2차 소유권 재검증 · 같은 트랜잭션 내 감소+승격
 * · 마감 강좌 승격 동결)와 승격 헬퍼 {@link #promoteNextEligible}(부적격
 * 대기자 건너뛰기, REQ-WL-009)을 구현한다.</p>
 *
 * <p><b>M5 범위</b>(plan.md §F M5, REQ-ADX-001~005): {@code CAPACITY_INCREASE}
 * 요청의 전체 디스패치({@link #dispatchCapacityIncrease})를 구현한다 — 마감
 * 강좌 승격 동결(REQ-ADX-005)을 가장 먼저 확인한 뒤, {@link
 * #promoteNextEligible}을 정원이 찰 때까지(또는 승격 대상이 소진될 때까지)
 * 반복 호출한다(design.md §4.3). {@code CANCEL}이 이 헬퍼를 최대 1회만
 * 호출하는 것과 달리 이 경로는 여러 명을 한 번에 승격할 수 있다는 점이
 * 유일한 차이이며, 헬퍼 자체는 수정하지 않고 그대로 재사용한다. 큐 적재는
 * {@code course.CourseService#update}가 {@link
 * com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService#receiveCapacityIncrease}를
 * 거쳐 수행한다 — 승격 처리를 관리자 API 경로에서 직접 하지 않는다
 * (REQ-ADX-001).</p>
 */
@Service
public class EnrollmentRequestProcessor {

    private static final Logger log = LoggerFactory.getLogger(EnrollmentRequestProcessor.class);

    private final EnrollmentRequestRepository requestRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final CourseCapacityRepository courseCapacityRepository;
    private final EnrollmentFailureInjector failureInjector;

    public EnrollmentRequestProcessor(EnrollmentRequestRepository requestRepository,
            EnrollmentRepository enrollmentRepository, WaitlistEntryRepository waitlistEntryRepository,
            CourseCapacityRepository courseCapacityRepository, EnrollmentFailureInjector failureInjector) {
        this.requestRepository = requestRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.courseCapacityRepository = courseCapacityRepository;
        this.failureInjector = failureInjector;
    }

    /**
     * 순서값 오름차순으로 최대 {@code limit}건의 미처리 요청 id를 클레임한다.
     * {@link EnrollmentQueueWorker}(별도 빈)가 자기 호출(self-invocation) 없이
     * 프록시를 경유해 호출한다 — 같은 빈 내부 호출은 {@code @Transactional}이
     * 적용되지 않는다(research.md §5). {@code FOR UPDATE}가 읽기 전용
     * 트랜잭션에서 거부될 수 있으므로 이 메서드는 readOnly를 지정하지 않는다.
     */
    @Transactional
    public List<Long> claimBatch(int limit) {
        return requestRepository.claimNextBatchIds(limit);
    }

    /**
     * 요청 1건을 하나의 트랜잭션으로 처리한다(REQ-WRK-010). 이미 처리된
     * ({@code state='DONE'}) 행이면 조용히 무동작한다(REQ-WRK-011 멱등성).
     * 디스패치 중 예외가 발생하면 이 메서드의 트랜잭션 전체가 롤백되어
     * 도메인 변경(확정 INSERT·정원 증가·대기 등록)이 사라진다 — 상태 전이
     * 기록({@code markDone})까지 함께 사라지므로, 호출부({@link
     * EnrollmentQueueWorker#drainQueue})가 예외를 잡아 {@link
     * #recordFailure}를 별도 트랜잭션으로 호출해야 요청이 {@code FAILED}로
     * 종결된다(research.md §5 "예외 처리 위치", AC-ENR-017).
     */
    @Transactional
    public void processOne(Long requestId) {
        EnrollmentRequest request = requestRepository.findPendingForUpdateSkipLocked(requestId).orElse(null);
        if (request == null) {
            return;
        }
        // @MX:NOTE: [AUTO] 요청 처리 시작·종료·결과를 요청 식별자와 함께 기록한다(REQ-NFR-004, AC-ENR-047). 순수 관찰 목적 로깅이며 반환값·트랜잭션 경계·제어 흐름은 변경하지 않는다.
        log.info("큐 요청 처리 시작 requestId={} requestType={}", requestId, request.getRequestType());
        RequestResult result = dispatch(request);
        request.markDone(result);
        log.info("큐 요청 처리 종료 requestId={} result={}", requestId, result);
    }

    /**
     * {@link #processOne}이 예외로 롤백된 뒤 호출된다. {@code REQUIRES_NEW}로
     * 완전히 새 트랜잭션을 열어 {@code state='DONE', result='FAILED'}만
     * 기록한다 — 같은 트랜잭션에서 기록하면 도메인 롤백과 함께 이 기록까지
     * 사라져 요청이 {@code PENDING}으로 영원히 남고 무한 재시도가 된다
     * (research.md §5, AC-ENR-017). 이 메서드는 {@link EnrollmentQueueWorker}
     * (별도 빈)에서만 호출되므로 자기 호출 문제 없이 프록시를 정상 경유한다.
     * 대상 행이 이미 다른 경로로 {@code DONE}이 되었으면(단일 워커 순차
     * 처리에서는 이론상 발생하지 않음) 조용히 무동작한다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long requestId) {
        requestRepository.findById(requestId)
                .filter(request -> request.getState() == RequestState.PENDING)
                .ifPresent(request -> {
                    request.markDone(RequestResult.FAILED);
                    log.info("큐 요청 처리 종료 requestId={} result={}", requestId, RequestResult.FAILED);
                });
    }

    private RequestResult dispatch(EnrollmentRequest request) {
        return switch (request.getRequestType()) {
            case ENROLL -> dispatchEnroll(request);
            case CANCEL -> dispatchCancel(request);
            case CAPACITY_INCREASE -> dispatchCapacityIncrease(request);
        };
    }

    /**
     * REQ-WRK-004~007의 {@code ENROLL} 디스패치 전체 — 마감 → 중복 3종 →
     * 확정/대기. 순서는 design.md §4.3 의사코드와 동일하다: 마감 여부를 가장
     * 먼저 확인해야 마감된 강좌에서 중복 검사·정원 검사 결과가 의미를 갖지
     * 않는 상황을 피할 수 있다(AC-ENR-013).
     */
    private RequestResult dispatchEnroll(EnrollmentRequest request) {
        Course course = courseCapacityRepository.findById(request.getCourseId())
                .orElseThrow(() -> new IllegalStateException(
                        "워커 처리 시점에 강좌를 찾을 수 없습니다: " + request.getCourseId()));

        if (course.getStatus() == CourseStatus.CLOSED) {
            return RequestResult.CLOSED;
        }

        if (isDuplicateEnroll(request)) {
            return RequestResult.REJECTED;
        }

        int updated = courseCapacityRepository.incrementEnrolledCountIfAvailable(request.getCourseId());
        if (updated == 1) {
            enrollmentRepository.save(Enrollment.create(request.getMemberId(), request.getCourseId()));
            // AC-ENR-016/017 재현용 테스트 훅 — 프로덕션 기본 구현은 무동작이다
            // (EnrollmentFailureInjector 클래스 주석 참고).
            failureInjector.afterEnrollmentPersisted(request.getId());
            return RequestResult.SUCCESS;
        }

        long nextPosition = waitlistEntryRepository.nextPosition(request.getCourseId());
        waitlistEntryRepository.save(
                WaitlistEntry.create(request.getMemberId(), request.getCourseId(), nextPosition));
        return RequestResult.WAITLISTED;
    }

    /**
     * REQ-WRK-007 중복 검사 3종 — (1) 이미 유효한 확정 보유, (2) 이 요청
     * 자신을 제외한 미처리({@code PENDING}) 큐 요청 보유, (3) 활성
     * ({@code WAITING}) 대기 항목 보유. 세 번째 검사가 없으면 이미 대기자가
     * 된 회원의 재신청이 통과해 같은 강좌의 대기 순번을 2개 점유한다
     * (spec.md §B.2 REQ-WRK-007 주석, 2차 감사 E1).
     */
    private boolean isDuplicateEnroll(EnrollmentRequest request) {
        Long memberId = request.getMemberId();
        Long courseId = request.getCourseId();
        return enrollmentRepository.existsByMemberIdAndCourseIdAndStatus(
                    memberId, courseId, EnrollmentStatus.ENROLLED)
                || requestRepository.existsByMemberIdAndCourseIdAndStateAndIdNot(
                        memberId, courseId, RequestState.PENDING, request.getId())
                || waitlistEntryRepository.existsByMemberIdAndCourseIdAndStatus(
                        memberId, courseId, WaitlistStatus.WAITING);
    }

    /**
     * {@code CANCEL} 디스패치 — 이 SPEC에서 가장 중요한 설계 판단(design.md
     * §4.3). 취소·{@code enrolled_count} 감소·승격을 {@link #processOne}이
     * 여는 트랜잭션 1개 안에서 연속 수행한다 — 감소가 커밋되는 시점에는
     * 승격도 이미 커밋되어 있으므로 여유 정원이 외부에 노출되는 시간 창이
     * 존재하지 않는다(REQ-WL-004, AC-ENR-031). 대상이 없거나 이미 취소됐거나
     * 소유자가 아니면(2차 소유권 재검증, REQ-CNL-003 — API 계층 우회 대비
     * 방어선, AC-ENR-037) 어떤 도메인 변경도 없이 {@code REJECTED}로 종결한다.
     */
    private RequestResult dispatchCancel(EnrollmentRequest request) {
        Enrollment target = enrollmentRepository.findById(request.getTargetEnrollmentId()).orElse(null);
        if (target == null || target.getStatus() != EnrollmentStatus.ENROLLED) {
            return RequestResult.REJECTED;
        }
        if (!target.getMemberId().equals(request.getMemberId())) {
            return RequestResult.REJECTED;
        }

        target.cancel();
        Long courseId = request.getCourseId();
        courseCapacityRepository.decrementEnrolledCountIfPositive(courseId);

        Course course = courseCapacityRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException(
                        "워커 처리 시점에 강좌를 찾을 수 없습니다: " + courseId));
        if (course.getStatus() == CourseStatus.CLOSED) {
            // 마감 강좌에서는 취소만 수행하고 승격은 동결한다(REQ-WL-011,
            // spec.md §A.5, AC-ENR-052). 대기명단 순번은 그대로 보존된다.
            return RequestResult.CANCELLED;
        }

        promoteNextEligible(courseId);
        return RequestResult.CANCELLED;
    }

    /**
     * REQ-ADX-002·003·005의 {@code CAPACITY_INCREASE} 디스패치 전체 — 마감
     * 분기가 {@link #dispatchCancel}과 동일한 이유로 승격 시도보다 먼저다
     * (REQ-ADX-005, spec.md §A.5, AC-ENR-053). 마감이 아니면 {@link
     * #promoteNextEligible}이 더 이상 승격할 대상이 없다고 알릴 때까지(정원
     * 도달 또는 활성 대기자 소진, 둘 다 헬퍼 내부의 원자적 정원 게이트가
     * 처리한다) 반복 호출한다 — {@code CANCEL} 경로가 최대 1명만 승격하는
     * 것과 달리 이 경로는 여러 명을 한 번에 승격할 수 있다(design.md §4.3,
     * AC-ENR-042). 한 명도 승격하지 못하면 {@code NOOP}(REQ-ADX-003), 1명
     * 이상이면 {@code PROMOTED}로 종결한다.
     */
    private RequestResult dispatchCapacityIncrease(EnrollmentRequest request) {
        Long courseId = request.getCourseId();
        Course course = courseCapacityRepository.findById(courseId)
                .orElseThrow(() -> new IllegalStateException(
                        "워커 처리 시점에 강좌를 찾을 수 없습니다: " + courseId));

        if (course.getStatus() == CourseStatus.CLOSED) {
            // 마감 강좌에서는 어떤 승격도 하지 않는다(REQ-ADX-005, spec.md
            // §A.5, AC-ENR-053). 대기명단 순번은 그대로 보존된다.
            return RequestResult.CLOSED;
        }

        boolean promotedAny = false;
        while (promoteNextEligible(courseId)) {
            promotedAny = true;
        }
        return promotedAny ? RequestResult.PROMOTED : RequestResult.NOOP;
    }

    /**
     * 승격 헬퍼 — design.md §4.3 {@code promoteNextEligible}. 가장 앞선 활성
     * 대기자의 승격 적격 여부를 확인하고, 부적격(이미 이 강좌에 유효한 확정
     * 보유)이면 {@code DUPLICATE}로 종결한 뒤 다음 대기자로 건너뛴다 —
     * 예외를 던져 {@link #processOne} 트랜잭션 전체를 롤백시키지 않는다.
     *
     * <p>{@code CANCEL} 처리는 이 메서드를 1회만 호출한다(승격 최대 1명).
     * {@code CAPACITY_INCREASE}({@link #dispatchCapacityIncrease}, M5)는
     * 이 메서드가 {@code false}를 반환할 때까지(정원 도달 또는 활성 대기자
     * 소진) 반복 호출해 같은 헬퍼를 그대로 재사용한다.</p>
     */
    // @MX:ANCHOR: [AUTO] 부적격 대기자는 예외를 던지지 않고 건너뛴다 — 승격 INSERT를 적격성 확인 뒤에만 수행한다
    // @MX:REASON: REQ-WL-009(2차 감사 E1) — 검사 없이 곧장 INSERT하면 (member_id, course_id) WHERE status='ENROLLED' 부분 유니크 인덱스 위반으로 processOne 트랜잭션 전체가 롤백된다. 그 결과 CANCEL이 소실되고 여유 정원이 영원히 재배정되지 않으며, 부적격 항목이 대기 선두에 남아 이후 모든 CANCEL을 같은 방식으로 실패시킨다 — 큐 선두 영구 정지(생존성 결함). AC-ENR-044가 이 계약을 정확히 검증한다.
    private boolean promoteNextEligible(Long courseId) {
        while (true) {
            WaitlistEntry front = waitlistEntryRepository
                    .findFirstByCourseIdAndStatusOrderByPositionAsc(courseId, WaitlistStatus.WAITING)
                    .orElse(null);
            if (front == null) {
                return false;
            }

            boolean ineligible = enrollmentRepository.existsByMemberIdAndCourseIdAndStatus(
                    front.getMemberId(), courseId, EnrollmentStatus.ENROLLED);
            if (ineligible) {
                front.markDuplicate();
                continue;
            }

            int updated = courseCapacityRepository.incrementEnrolledCountIfAvailable(courseId);
            if (updated == 0) {
                // 정원이 이미 가득 찬 경우 — 승격하지 않는다(REQ-WL-006, 3중
                // 방어선 2번째 층). 단일 워커 순차 처리 전제에서는 CANCEL이
                // 직전에 1명분을 비웠으므로 이 경로는 이론상 도달하지 않지만
                // 방어적으로 유지한다(design.md §2).
                return false;
            }
            enrollmentRepository.save(Enrollment.create(front.getMemberId(), courseId));
            front.promote();
            return true;
        }
    }
}
