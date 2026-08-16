package com.hongseob.openclass_ap.course.admin;

import com.hongseob.openclass_ap.course.CourseService;
import com.hongseob.openclass_ap.course.dto.CourseCreateRequest;
import com.hongseob.openclass_ap.course.dto.CourseResponse;
import com.hongseob.openclass_ap.course.dto.CourseUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 강좌 관리 API (plan.md §C.2 — 생성·수정·마감·soft 삭제). {@code admin}은
 * 별도 도메인이 아니라 인가 관점이므로 {@code Course} 애그리게이트를 다루는
 * {@link CourseService}를 그대로 재사용한다(plan.md §C.4).
 *
 * <p>인가는 {@code SecurityConfig}의 기존 {@code /api/admin/**} → hasRole("ADMIN")
 * 규칙이 처리한다 — 이 컨트롤러는 새 인가 규칙을 정의하지 않는다(REQ-ADM-002,
 * plan.md §C.2 근거). {@code MEMBER} 역할 토큰의 403 응답은 그 기존 규칙이
 * 만들어내는 결과이며 이 클래스는 그 결과만 관찰 대상이 된다(AC-ADM-002).</p>
 */
@RestController
@RequestMapping("/api/admin/courses")
public class CourseAdminController {

    private final CourseService courseService;

    public CourseAdminController(CourseService courseService) {
        this.courseService = courseService;
    }

    @PostMapping
    public ResponseEntity<CourseResponse> create(@Valid @RequestBody CourseCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CourseResponse> update(@PathVariable Long id, @Valid @RequestBody CourseUpdateRequest request) {
        return ResponseEntity.ok(courseService.update(id, request));
    }

    @PostMapping("/{id}/close")
    public ResponseEntity<CourseResponse> close(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.close(id));
    }

    /**
     * "삭제" API — 물리 삭제 경로를 만들지 않고 {@link CourseService#close}가
     * 수행하는 것과 동일한 마감 전이를 그대로 재사용한다(REQ-ADM-008,
     * plan.md §C.1 설계 판단 4 / §C.2 "soft — 내부적으로 마감 전이"). 이 클래스에는
     * {@code course}에 대한 delete/remove 호출이 없다(AC-ADM-007).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<CourseResponse> delete(@PathVariable Long id) {
        return ResponseEntity.ok(courseService.close(id));
    }
}
