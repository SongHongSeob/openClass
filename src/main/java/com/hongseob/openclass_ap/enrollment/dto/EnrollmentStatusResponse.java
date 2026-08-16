package com.hongseob.openclass_ap.enrollment.dto;

/**
 * 상태 조회 응답 바디 (M3, design.md §8 — {@code GET
 * /api/enrollment-requests/{requestId}}). {@code status}는 spec.md §A.4.1
 * "클라이언트 노출 상태"를 그대로 옮긴다 — {@code state='PENDING'}이면
 * {@code "PENDING"}, 아니면 {@code result} 값 그대로(REQ-STS-001).
 *
 * <p>{@code waitlistPosition}은 {@code status}가 {@code "WAITLISTED"}일 때만
 * 채워진다. 그 외에는 {@code null}이다 — 대기명단 항목이 없거나 이미 종단
 * 상태(승격·취소)가 된 경우를 의미 있게 구분하지 않는다(그 구분은 이 SPEC의
 * 범위가 아니다).</p>
 */
public record EnrollmentStatusResponse(Long requestId, String status, Long waitlistPosition) {
}
