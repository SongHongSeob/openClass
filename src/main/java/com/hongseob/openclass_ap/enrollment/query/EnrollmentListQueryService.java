package com.hongseob.openclass_ap.enrollment.query;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.enrollment.Enrollment;
import com.hongseob.openclass_ap.enrollment.EnrollmentRepository;
import com.hongseob.openclass_ap.enrollment.EnrollmentStatus;
import com.hongseob.openclass_ap.enrollment.dto.EnrollmentListItemResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 내 확정 수강신청 목록 조회(M7, v0.3.0 개정 — spec.md §A.6, plan.md §C.8 결정 4).
 *
 * <p>부작용 없는 읽기 전용 조회만 수행한다(REQ-LST-004, AC-ENR-058) — {@link
 * EnrollmentStatusQueryService}가 상태 조회 경로에 세운 것과 동일한 원칙을 이
 * 목록 조회 경로에도 적용한다. 확정 생성·{@code enrolled_count} 변경 등 어떤
 * 도메인 변경도 이 클래스가 수행하지 않는다 — 그것은 여전히 워커의 유일한
 * 책임이다(REQ-WRK-001/002, INV-ENR-002).</p>
 *
 * <p>{@code courseTitle}은 {@link Enrollment}에 JPA 연관을 추가하지 않고 기존
 * {@link CourseRepository}로 별도 배치 조회하여 합친다(plan.md §C.8 결정 3) —
 * {@code Enrollment}를 향한 JPA 연관은 §C.4의 3층 검증(AC-ENR-009 (i) 연쇄 저장
 * 0건)의 전제를 흔든다.</p>
 *
 * <p>소유권 범위 한정은 호출부(컨트롤러)가 {@code Authentication}에서 유도한
 * 회원 식별자를 전달하는 것으로 끝난다 — 이 클래스도 회원 식별자를 검사로
 * 걸러내는 것이 아니라, 애초에 다른 회원의 데이터를 지목할 입력 자체를 받지
 * 않는다(spec.md §A.6.4, INV-ENR-010).</p>
 */
@Service
public class EnrollmentListQueryService {

    private final EnrollmentRepository enrollmentRepository;
    private final CourseRepository courseRepository;

    public EnrollmentListQueryService(EnrollmentRepository enrollmentRepository, CourseRepository courseRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.courseRepository = courseRepository;
    }

    /**
     * 요청자 본인의 활성 확정 수강신청({@code status='ENROLLED'})만 {@code
     * enrollmentId} 오름차순으로 반환한다(REQ-LST-001). 대상이 0건이면 빈
     * 목록을 반환한다 — 404가 아니다.
     */
    @Transactional(readOnly = true)
    public List<EnrollmentListItemResponse> listMine(Long memberId) {
        List<Enrollment> enrollments =
                enrollmentRepository.findByMemberIdAndStatusOrderByIdAsc(memberId, EnrollmentStatus.ENROLLED);

        Map<Long, String> courseTitleById = courseTitleById(enrollments);

        return enrollments.stream()
                .map(enrollment -> new EnrollmentListItemResponse(
                        enrollment.getId(),
                        enrollment.getCourseId(),
                        courseTitleById.get(enrollment.getCourseId()),
                        enrollment.getStatus().name(),
                        enrollment.getEnrolledAt()))
                .toList();
    }

    private Map<Long, String> courseTitleById(List<Enrollment> enrollments) {
        List<Long> courseIds = enrollments.stream().map(Enrollment::getCourseId).distinct().toList();
        return courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle, (left, right) -> left));
    }
}
