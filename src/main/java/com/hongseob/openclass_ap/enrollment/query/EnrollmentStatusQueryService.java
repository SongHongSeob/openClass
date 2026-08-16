package com.hongseob.openclass_ap.enrollment.query;

import com.hongseob.openclass_ap.common.exception.EnrollmentRequestNotFoundException;
import com.hongseob.openclass_ap.enrollment.dto.EnrollmentStatusResponse;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequest;
import com.hongseob.openclass_ap.enrollment.request.EnrollmentRequestRepository;
import com.hongseob.openclass_ap.enrollment.request.RequestResult;
import com.hongseob.openclass_ap.enrollment.request.RequestState;
import com.hongseob.openclass_ap.waitlist.WaitlistEntry;
import com.hongseob.openclass_ap.waitlist.WaitlistEntryRepository;
import com.hongseob.openclass_ap.waitlist.WaitlistStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상태 조회 — 부작용 없는 읽기 전용 조회만 수행한다(REQ-STS-004, AC-ENR-027).
 * 확정 처리·정원 검사·대기명단 승격 등 어떤 도메인 변경도 이 클래스가 수행하지
 * 않는다 — 그것은 워커의 유일한 책임이다(REQ-WRK-001/002, INV-ENR-002. M1/M2가
 * 확립한 경계와 동일한 원칙을 조회 경로에도 적용한 것뿐이다).
 *
 * <p>소유권 검증(REQ-STS-002, AC-ENR-025)은 이 클래스가 담당한다 — 요청자의
 * 회원 식별자와 큐 행의 소유자가 일치하지 않으면 존재하지 않는 것과 동일하게
 * {@link EnrollmentRequestNotFoundException}을 던진다. 존재 여부 자체를 노출하지
 * 않기 위해 "찾을 수 없음"과 "권한 없음"을 구분하지 않는다.</p>
 */
@Service
public class EnrollmentStatusQueryService {

    private final EnrollmentRequestRepository requestRepository;
    private final WaitlistEntryRepository waitlistEntryRepository;

    public EnrollmentStatusQueryService(EnrollmentRequestRepository requestRepository,
            WaitlistEntryRepository waitlistEntryRepository) {
        this.requestRepository = requestRepository;
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    /**
     * spec.md §A.4.1 "클라이언트 노출 상태"를 그대로 반환한다 — {@code
     * state='PENDING'}이면 {@code "PENDING"}, 아니면 {@code result} 값
     * 그대로(REQ-STS-001). 결과가 {@code WAITLISTED}이면 대기 순번을 함께
     * 채운다(AC-ENR-024).
     */
    @Transactional(readOnly = true)
    public EnrollmentStatusResponse getStatus(Long requestId, Long memberId) {
        EnrollmentRequest request = requestRepository.findById(requestId)
                .filter(found -> found.getMemberId().equals(memberId))
                .orElseThrow(() -> new EnrollmentRequestNotFoundException(requestId));

        boolean pending = request.getState() == RequestState.PENDING;
        String clientStatus = pending ? "PENDING" : request.getResult().name();

        Long waitlistPosition = null;
        if (!pending && request.getResult() == RequestResult.WAITLISTED) {
            waitlistPosition = waitlistEntryRepository
                    .findByMemberIdAndCourseIdAndStatus(memberId, request.getCourseId(), WaitlistStatus.WAITING)
                    .map(WaitlistEntry::getPosition)
                    .orElse(null);
        }

        return new EnrollmentStatusResponse(request.getId(), clientStatus, waitlistPosition);
    }
}
