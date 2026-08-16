package com.hongseob.openclass_ap.waitlist;

import com.hongseob.openclass_ap.common.exception.WaitlistEntryNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대기명단 취소 — 대기자 본인의 대기 신청을 취소한다(M4, REQ-WL-007/008,
 * design.md §8). 큐를 경유하지 않고 즉시 처리한다 — {@code enrolled_count}를
 * 건드리지 않으므로 정원 카운터 변경 경로 단일화(REQ-WRK-002)의 대상이
 * 아니다(design.md §8 "대기 취소가 큐를 경유하지 않는 이유").
 *
 * <p>순번 부여·조회는 {@link WaitlistEntryRepository}가 직접 담당한다
 * (design.md §7 — "순번 관리와 조회만 담당. {@code Enrollment}를 생성하지
 * 않는다"). 이 클래스는 그 원칙 위에 취소 유스케이스만 추가한다 — 확정
 * 생성 권한은 여전히 없다.</p>
 */
@Service
public class WaitlistService {

    private final WaitlistEntryRepository waitlistEntryRepository;

    public WaitlistService(WaitlistEntryRepository waitlistEntryRepository) {
        this.waitlistEntryRepository = waitlistEntryRepository;
    }

    /**
     * 대기명단 항목을 취소한다(REQ-WL-007). 소유자가 아니거나 존재하지
     * 않거나 이미 종단 상태(승격·취소·중복 종결)인 항목이면 {@link
     * WaitlistEntryNotFoundException}을 던진다 — "찾을 수 없음"과 "권한
     * 없음"을 구분하지 않는다(REQ-WL-008, AC-ENR-034 — {@code
     * EnrollmentRequestNotFoundException}과 동일한 IDOR 방지 원칙).
     *
     * <p>뒤 순번 대기자들의 상대 순서는 건드리지 않는다 — {@code position}
     * 값을 재계산하지 않고 이 항목만 {@code CANCELLED}로 전이한다
     * (AC-ENR-033).</p>
     */
    @Transactional
    public void cancel(Long entryId, Long memberId) {
        WaitlistEntry entry = waitlistEntryRepository.findById(entryId)
                .filter(found -> found.getMemberId().equals(memberId))
                .filter(found -> found.getStatus() == WaitlistStatus.WAITING)
                .orElseThrow(() -> new WaitlistEntryNotFoundException(entryId));
        entry.cancel();
    }
}
