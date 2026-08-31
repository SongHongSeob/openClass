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
 *
 * <p>{@code capacity}/{@code enrolledCount}/{@code remainingCapacity}/{@code
 * courseStatus}는 이 수강신청이 속한 강좌의 정원 현황이다 — {@link
 * com.hongseob.openclass_ap.course.dto.CourseResponse}와 동일한 계산으로
 * {@code remainingCapacity = capacity - enrolledCount}를 이 매핑 계층에서
 * 계산하며 저장하지 않는다. {@code courseStatus}는 강좌의 OPEN/CLOSED
 * 상태이며, 이 record의 기존 {@code status} 필드(수강신청 자체의 상태)와는
 * 별개다.</p>
 */
public record EnrollmentListItemResponse(
        Long enrollmentId,
        Long courseId,
        String courseTitle,
        String status,
        LocalDateTime enrolledAt,
        Integer capacity,
        Integer enrolledCount,
        Integer remainingCapacity,
        String courseStatus
) {
}
