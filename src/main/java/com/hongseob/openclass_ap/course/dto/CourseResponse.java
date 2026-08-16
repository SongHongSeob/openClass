package com.hongseob.openclass_ap.course.dto;

import com.hongseob.openclass_ap.course.Course;
import java.time.LocalDateTime;

/**
 * 공개 카탈로그 목록·상세 조회 공통 응답 바디 (plan.md §C.4.1-1 — DTO는 전부 record).
 *
 * <p>잔여 정원({@code remainingCapacity})은 {@code capacity - enrolledCount}로 이 매핑
 * 계층에서 계산하며 저장하지 않는다(REQ-CAT-003, plan.md §C.1 설계 판단 2). 목록·상세
 * 양쪽에 동일한 필드 집합이면 충분하므로 별도 요약/상세 record로 분리하지 않는다.</p>
 */
public record CourseResponse(
        Long id,
        String title,
        String description,
        Integer capacity,
        Integer enrolledCount,
        Integer remainingCapacity,
        LocalDateTime startsAt,
        LocalDateTime endsAt,
        String status
) {

    public static CourseResponse from(Course course) {
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription(),
                course.getCapacity(),
                course.getEnrolledCount(),
                course.getCapacity() - course.getEnrolledCount(),
                course.getStartsAt(),
                course.getEndsAt(),
                course.getStatus().name()
        );
    }
}
