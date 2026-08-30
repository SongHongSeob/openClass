package com.hongseob.openclass_ap.course;

import com.hongseob.openclass_ap.common.exception.CapacityBelowEnrollmentException;
import com.hongseob.openclass_ap.common.exception.CourseNotFoundException;
import com.hongseob.openclass_ap.course.dto.CourseCreateRequest;
import com.hongseob.openclass_ap.course.dto.CoursePageResponse;
import com.hongseob.openclass_ap.course.dto.CourseResponse;
import com.hongseob.openclass_ap.course.dto.CourseUpdateRequest;
import com.hongseob.openclass_ap.enrollment.receipt.EnrollmentReceiptService;
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
 *
 * <p><b>M5 훅</b>(REQ-ADX-001, {@code SPEC-ENROLLMENT-001}): {@link #update}가
 * 정원이 실제로 증가한 경우에만(신규 정원 &gt; 이전 정원) {@link
 * EnrollmentReceiptService#receiveCapacityIncrease}를 호출해 {@code
 * CAPACITY_INCREASE} 요청을 큐에 적재한다. 이 클래스는 승격을 직접 수행하지
 * 않는다 — 적재만 위임하고 그 결과를 기다리지 않는다(plan.md §G 안티패턴
 * "정원 증설을 관리자 API에서 직접 승격 처리").</p>
 */
@Service
@Transactional(readOnly = true)
public class CourseService {

    private final CourseRepository courseRepository;
    private final EnrollmentReceiptService enrollmentReceiptService;

    public CourseService(CourseRepository courseRepository, EnrollmentReceiptService enrollmentReceiptService) {
        this.courseRepository = courseRepository;
        this.enrollmentReceiptService = enrollmentReceiptService;
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
     * 강좌 목록을 강좌명 키워드로 검색해 페이지 단위로 조회한다(REQ-CAT-007,
     * SPEC-COURSE-001 Amendment 1). {@code keyword}가 {@code null}이거나
     * 공백뿐이면 {@link #list(int, int)}와 동일하게 전체 목록을 반환한다 —
     * 기존 호출자의 동작을 변경하지 않는다(추가 전용 변경).
     */
    public CoursePageResponse list(int page, int size, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return list(page, size);
        }
        Page<Course> coursePage = courseRepository.findByTitleContainingIgnoreCase(keyword, PageRequest.of(page, size));
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
     *
     * <p>정원이 실제로 증가한 경우에만(신규 정원 &gt; 이전 정원) {@code
     * CAPACITY_INCREASE} 요청을 큐에 적재한다(REQ-ADX-001, AC-ENR-041) — 무변경
     * 갱신이나 축소(§C.3 검증을 통과한 경우, 즉 확정 인원까지의 축소)에서는
     * 적재하지 않는다. 승격은 이 메서드가 반환한 뒤 워커가 비동기로 수행한다.</p>
     */
    @Transactional
    public CourseResponse update(Long id, CourseUpdateRequest request) {
        Course course = courseRepository.findById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        if (request.capacity() < course.getEnrolledCount()) {
            throw new CapacityBelowEnrollmentException(id, request.capacity(), course.getEnrolledCount());
        }
        Integer previousCapacity = course.getCapacity();
        course.updateDetails(request.title(), request.description(), request.capacity(),
                request.startsAt(), request.endsAt());
        if (request.capacity() > previousCapacity) {
            enrollmentReceiptService.receiveCapacityIncrease(id);
        }
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
