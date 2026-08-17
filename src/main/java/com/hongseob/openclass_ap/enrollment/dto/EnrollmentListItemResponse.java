package com.hongseob.openclass_ap.enrollment.dto;

import java.time.LocalDateTime;

/**
 * 내 확정 수강신청 목록 조회 응답 항목 (M7, v0.3.0 개정 — spec.md §A.6.2,
 * REQ-LST-001).
 *
 * <p>{@code enrollmentId}는 {@link com.hongseob.openclass_ap.enrollment.Enrollment}
 * 행 자체의 식별자다 — 큐 요청의 {@code requestId}가 아니다. 이 값을 그대로
 * {@code DELETE /api/enrollments/{enrollmentId}}에 넣으면 취소가 성립해야
 * 한다는 것이 이 개정의 종결 조건이다(REQ-LST-006, AC-ENR-056).</p>
 */
public record EnrollmentListItemResponse(
        Long enrollmentId,
        Long courseId,
        String courseTitle,
        String status,
        LocalDateTime enrolledAt
) {
}
