package com.hongseob.openclass_ap.enrollment.worker;

import com.hongseob.openclass_ap.enrollment.Enrollment;
import com.hongseob.openclass_ap.enrollment.EnrollmentRepository;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.request.RequestType;
import com.hongseob.openclass_ap.waitlist.WaitlistEntry;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 큐 요청 처리 — 클레임 + 요청 1건 처리(design.md §4.1-4.2).
 *
 * <p><b>M1 범위 축소 고지 (M1/M2 경계)</b>: 이 클래스는 M1의 AC-ENR-006/007
 * (커밋 지연 하 접수 순서 보장 + 잠금 제거 대조군)을 관측 가능하게 만드는 데
 * 필요한 최소한의 {@code ENROLL} 디스패치만 구현한다 — 정원 여유가 있으면
 * 확정(SUCCESS), 없으면 대기 등록(WAITLISTED)으로 분기하는 것이 전부다. 아래는
 * 의도적으로 M1 범위 밖이며 M2가 추가한다(plan.md §F M2, REQ-WRK-001~015):</p>
 * <ul>
 *   <li>마감 강좌 분기({@code CLOSED} 결과) — REQ-WRK-006</li>
 *   <li>중복 신청 3종 검사({@code REJECTED} 결과) — REQ-WRK-007</li>
 *   <li>{@code CANCEL}·{@code CAPACITY_INCREASE} 디스패치 — M4/M5</li>
 *   <li>실패 격리({@code REQUIRES_NEW} 트랜잭션의 {@code FAILED} 기록) — REQ-WRK-009/010</li>
 *   <li>매핑 제약 + ArchUnit + DB 상태 단언 3층 검증(확정 경로 단일성 전체 증명) — design.md §5, AC-ENR-008/009</li>
 * </ul>
 * <p>이 스텁이 처리하는 {@code CANCEL}/{@code CAPACITY_INCREASE} 요청은 아직
 * 프로덕션 경로로 생성되지 않으므로({@link
 * com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService}가
 * {@code ENROLL}만 적재) {@link #dispatch}가 그 종류를 만나면 방어적으로
 * 예외를 던진다 — 조용히 잘못된 결과를 기록하는 것보다 낫다.</p>
 */
@Service
public class EnrollmentRequestProcessor {

    private final EnrollmentRequestRepository requestRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;
    private final CourseCapacityRepository courseCapacityRepository;

    public EnrollmentRequestProcessor(EnrollmentRequestRepository requestRepository,
            EnrollmentRepository enrollmentRepository, WaitlistEntryRepository waitlistEntryRepository,
            CourseCapacityRepository courseCapacityRepository) {
        this.requestRepository = requestRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
        this.courseCapacityRepository = courseCapacityRepository;
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
     * 요청 1건을 하나의 트랜잭션으로 처리한다(REQ-WRK-010 부분 준수 — 이
     * 스텁은 실패 격리를 아직 구현하지 않는다, 클래스 주석 참고). 이미
     * 처리된(state='DONE') 행이면 조용히 무동작한다(REQ-WRK-011 멱등성).
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

    private RequestResult dispatch(EnrollmentRequest request) {
        if (request.getRequestType() != RequestType.ENROLL) {
            // M1은 ENROLL만 적재하는 프로덕션 경로만 갖는다 — 여기 도달하면
            // 스키마를 우회한 직접 INSERT이거나 M4/M5가 아직 이 디스패치를
            // 확장하지 않은 상태에서 호출된 것이다.
            throw new UnsupportedOperationException(
                    "M1 워커 스텁은 ENROLL만 처리한다 — CANCEL/CAPACITY_INCREASE 디스패치는 M4/M5 범위: "
                            + request.getRequestType());
        }
        return dispatchEnroll(request);
    }

    /**
     * 정원 여유가 있으면 확정, 없으면 대기 등록으로 분기한다(REQ-WRK-004/005의
     * 최소 부분집합). {@link CourseCapacityRepository#incrementEnrolledCountIfAvailable}의
     * {@code WHERE enrolled_count < capacity} 가드가 원자적 정원 게이트다 —
     * 반환된 갱신 행 수로 확정 성공 여부를 판정하므로 별도의 선행 읽기가
     * 필요 없다.
     */
    private RequestResult dispatchEnroll(EnrollmentRequest request) {
        int updated = courseCapacityRepository.incrementEnrolledCountIfAvailable(request.getCourseId());
        if (updated == 1) {
            enrollmentRepository.save(Enrollment.create(request.getMemberId(), request.getCourseId()));
            return RequestResult.SUCCESS;
        }

        long nextPosition = waitlistEntryRepository.nextPosition(request.getCourseId());
        waitlistEntryRepository.save(
                WaitlistEntry.create(request.getMemberId(), request.getCourseId(), nextPosition));
        return RequestResult.WAITLISTED;
    }
}
