package com.hongseob.openclass_ap.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link Enrollment} 저장소. 상태 조회(M3)·취소(M4) 전용 조회 메서드는 해당
 * 마일스톤에서 추가한다(course.CourseRepository와 동일한 얇은 스타일).
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    /**
     * REQ-WRK-007 중복 검사 1번 — 동일 회원이 동일 강좌에 이미 유효한
     * 확정({@link EnrollmentStatus#ENROLLED})을 보유하는지 확인한다(M2,
     * design.md §4.3).
     */
    boolean existsByMemberIdAndCourseIdAndStatus(Long memberId, Long courseId, EnrollmentStatus status);
}
