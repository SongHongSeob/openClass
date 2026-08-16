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
 * <p>이 클래스가 처리하는 {@code CANCEL}/{@code CAPACITY_INCREASE} 요청은
 * 아직 프로덕션 경로로 생성되지 않으므로({@link
 * com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService}가
 * {@code ENROLL}만 적재) {@link #dispatch}가 그 종류를 만나면 방어적으로
 * 예외를 던진다 — 조용히 잘못된 결과를 기록하는 것보다 낫다. 각각의 디스패치는
 * M4({@code CANCEL})·M5({@code CAPACITY_INCREASE})가 추가한다.</p>
 */
@Service
public class EnrollmentRequestProcessor {

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
        RequestResult result = dispatch(request);
        request.markDone(result);
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
                .ifPresent(request -> request.markDone(RequestResult.FAILED));
    }

    private RequestResult dispatch(EnrollmentRequest request) {
        if (request.getRequestType() != RequestType.ENROLL) {
            // 이 요청 종류를 큐에 적재하는 프로덕션 경로는 아직 없다(CANCEL은
            // M4, CAPACITY_INCREASE는 M5 범위) — 여기 도달하면 스키마를
            // 우회한 직접 INSERT이거나 아직 확장되지 않은 상태에서 호출된
            // 것이다.
            throw new UnsupportedOperationException(
                    "CANCEL/CAPACITY_INCREASE 디스패치는 이 마일스톤 범위 밖이다: " + request.getRequestType());
        }
        return dispatchEnroll(request);
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
}
