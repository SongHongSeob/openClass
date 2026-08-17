package com.hongseob.openclass_ap.waitlist.dto;

/**
 * 내 대기명단 항목 목록 조회 응답 항목 (M7, v0.3.0 개정 — spec.md §A.6.2,
 * REQ-LST-002).
 *
 * <p>{@code waitlistEntryId}는 {@link com.hongseob.openclass_ap.waitlist.WaitlistEntry}
 * 행 자체의 식별자다 — 강좌 내 대기 순번({@code position})이 아니다. 이 둘을
 * 혼동하면 {@code DELETE /api/waitlist-entries/{entryId}}가 엉뚱한 행을
 * 지목하게 된다(REQ-LST-006, AC-ENR-055/056).</p>
 */
public record WaitlistListItemResponse(
        Long waitlistEntryId,
        Long courseId,
        String courseTitle,
        Long position,
        String status
) {
}
