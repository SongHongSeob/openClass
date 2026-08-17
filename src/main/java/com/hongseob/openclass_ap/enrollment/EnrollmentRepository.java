package com.hongseob.openclass_ap.enrollment;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@link Enrollment} 저장소. 상태 조회(M3)·취소(M4) 전용 조회 메서드는 해당
 * 마일스톤에서 추가한다(course.CourseRepository와 동일한 얇은 스타일).
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * REQ-WRK-007 중복 검사 1번 — 동일 회원이 동일 강좌에 이미 유효한
     * 확정({@link EnrollmentStatus#ENROLLED})을 보유하는지 확인한다(M2,
     * design.md §4.3). M4의 승격 헬퍼(promoteNextEligible)가 REQ-WL-009의
     * 부적격 판정에도 이 메서드를 그대로 재사용한다.
     */
    boolean existsByMemberIdAndCourseIdAndStatus(Long memberId, Long courseId, EnrollmentStatus status);

    /**
     * 취소 접수의 1차 소유권 검증(REQ-CNL-002, 감사 D12, AC-ENR-036)이 쓰는
     * 프로젝션 조회 — 존재·소유권·상태(ENROLLED)를 한 번에 판별하면서
     * {@code courseId}(접수 잠금 키)만 돌려준다. {@code
     * receipt.EnrollmentReceiptService}는 아키텍처 경계 규칙(AC-ENR-009)이
     * 워커 패키지로 한정한 {@link Enrollment} 엔티티 자체를 참조할 수
     * 없으므로, 이 저장소가 엔티티를 노출하지 않고 원시 값만 반환하는 이
     * 메서드로 그 경계를 지킨다(M4).
     */
    @Query("SELECT e.courseId FROM Enrollment e WHERE e.id = :id AND e.memberId = :memberId AND e.status = :status")
    Optional<Long> findCourseIdByIdAndMemberIdAndStatus(
            @Param("id") Long id, @Param("memberId") Long memberId, @Param("status") EnrollmentStatus status);

    /**
     * 내 확정 수강신청 목록 조회(M7, REQ-LST-001)가 쓰는 조회 메서드 — 요청자의
     * 활성 확정({@link EnrollmentStatus#ENROLLED}) 행만 {@code id} 오름차순으로
     * 반환한다(spec.md §A.6.2). 회원 식별자는 오직 인증 주체에서 유도한 값만
     * 전달되며, 이 메서드 자체는 어떤 열거 가능한 파라미터도 받지 않는다
     * (REQ-LST-003).
     */
    List<Enrollment> findByMemberIdAndStatusOrderByIdAsc(Long memberId, EnrollmentStatus status);
}
