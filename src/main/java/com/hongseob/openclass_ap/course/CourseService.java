package com.hongseob.openclass_ap.course;

import com.hongseob.openclass_ap.common.exception.CapacityBelowEnrollmentException;
import com.hongseob.openclass_ap.common.exception.CourseNotFoundException;
import com.hongseob.openclass_ap.course.dto.CourseCreateRequest;
import com.hongseob.openclass_ap.course.dto.CoursePageResponse;
import com.hongseob.openclass_ap.course.dto.CourseResponse;
import com.hongseob.openclass_ap.course.dto.CourseUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 강좌 카탈로그 조회 + 관리자 변경 유스케이스 (REQ-CAT-001 ~ 006, REQ-ADM-001 ~ 009).
 *
 * <p>{@code enrolled_count}를 변경하는 메서드는 정의하지 않는다(REQ-CRS-004 /
 * INV-CRS-003) — 이 클래스가 존재하는 한 계속 지켜야 할 제약이다. 아래 관리자
 * 메서드들은 강좌명·설명·정원·일정·상태만 변경하며 확정 인원에는 손대지 않는다.</p>
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

    /**
     * 강좌를 생성한다. {@link Course#create}가 확정 인원을 항상 0, 모집 상태를
     * 항상 {@code OPEN}으로 시작하므로 이 메서드는 그 위에 어떤 값도 덧씌우지
     * 않는다(REQ-ADM-001 / REQ-ADM-003, AC-ADM-001).
     */
    @Transactional
    public CourseResponse create(CourseCreateRequest request) {
        Course course = Course.create(request.title(), request.description(), request.capacity(),
                request.startsAt(), request.endsAt());
        Course saved = courseRepository.save(course);
        return CourseResponse.from(saved);
    }

    /**
     * 강좌명·설명·정원·일정을 수정한다. 정원을 확정 인원 미만으로 축소하는
     * 요청은 409로 거부한다 — 정확히 확정 인원과 같은 값으로의 축소는 허용이다
     * (REQ-ADM-005, plan.md §C.3, AC-ADM-004 경계값). 존재하지 않는 강좌 ID는
     * 404로 매핑되는 {@link CourseNotFoundException}을 던진다(REQ-ADM-009).
     */
    @Transactional
    public CourseResponse update(Long id, CourseUpdateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        if (request.capacity() < course.getEnrolledCount()) {
            throw new CapacityBelowEnrollmentException(id, request.capacity(), course.getEnrolledCount());
        }
        course.updateDetails(request.title(), request.description(), request.capacity(),
                request.startsAt(), request.endsAt());
        return CourseResponse.from(course);
    }

    /**
     * 강좌를 마감 상태로 전이한다(REQ-ADM-007). "삭제" API도 물리 삭제 대신 이
     * 메서드를 그대로 재사용한다(REQ-ADM-008, INV-CRS-004) — {@code course}에
     * 대한 delete/remove 호출은 이 클래스 어디에도 없다(AC-ADM-007).
     */
    @Transactional
    public CourseResponse close(Long id) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        course.close();
        return CourseResponse.from(course);
    }
}
