package com.hongseob.openclass_ap.course;

import com.hongseob.openclass_ap.course.dto.CoursePageResponse;
import com.hongseob.openclass_ap.course.dto.CourseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 공개 강좌 카탈로그 API (plan.md §C.2 — {@code GET /api/courses}, {@code GET
 * /api/courses/{id}}). 비인증 접근을 허용한다 — 인가 규칙은
 * {@code SecurityConfig}의 매처 확장(§C.2.1)이 담당하고, 이 컨트롤러는 결과만
 * 소비한다. 관리자 API는 M3의 {@code CourseAdminController}가 별도로 담당한다.
 */
@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping
    public ResponseEntity<CoursePageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(courseService.list(page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CourseResponse> detail(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.getDetail(id));
    }
}
