package com.hongseob.openclass_ap.course.dto;

import com.hongseob.openclass_ap.course.Course;
import java.util.List;
import org.springframework.data.domain.Page;

/**
 * 강좌 목록 페이지 응답 바디 (REQ-CAT-002 — 페이지네이션 메타데이터 포함).
 *
 * <p>{@code totalElements}·{@code totalPages}·{@code currentPage}는 실제
 * {@link Page}의 값을 그대로 전달한다 — AC-CAT-001의 경계 검증(페이지 분할이
 * 실제로 일어나는지)을 통과하려면 파라미터가 무시된 채 항상 전체를 반환하는
 * 구현이면 안 된다.</p>
 */
public record CoursePageResponse(
        List<CourseResponse> items,
        long totalElements,
        int totalPages,
        int currentPage
) {

    public static CoursePageResponse from(Page<Course> page) {
        List<CourseResponse> items = page.getContent().stream()
                .map(CourseResponse::from)
                .toList();
        return new CoursePageResponse(items, page.getTotalElements(), page.getTotalPages(), page.getNumber());
    }
}
