package com.hongseob.openclass_ap.course;

import com.hongseob.openclass_ap.common.exception.CourseNotFoundException;
import com.hongseob.openclass_ap.course.dto.CoursePageResponse;
import com.hongseob.openclass_ap.course.dto.CourseResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 강좌 카탈로그 조회 유스케이스 (REQ-CAT-001 ~ 006). M2는 조회만 다룬다 — 관리자
 * 변경 메서드는 M3에서 이 클래스에 추가한다(plan.md §F M3).
 *
 * <p>{@code enrolled_count}를 변경하는 메서드는 정의하지 않는다(REQ-CRS-004 /
 * INV-CRS-003) — 이 클래스가 존재하는 한 계속 지켜야 할 제약이다.</p>
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * 강좌 목록을 페이지 단위로 조회한다. {@code CLOSED} 강좌도 필터링 없이 그대로
     * 포함된다(REQ-CAT-006 / AC-CAT-005).
     */
    public CoursePageResponse list(int page, int size) {
        Page<Course> coursePage = courseRepository.findAll(PageRequest.of(page, size));
        return CoursePageResponse.from(coursePage);
    }

    /**
     * 강좌 상세를 조회한다. 존재하지 않으면 404로 매핑되는
     * {@link CourseNotFoundException}을 던진다(REQ-CAT-004).
     */
    public CourseResponse getDetail(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        return CourseResponse.from(course);
    }
}
