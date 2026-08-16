package com.hongseob.openclass_ap.enrollment;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link Enrollment} 저장소. M1은 워커 스텁이 확정 생성에 사용하는 최소
 * 형태만 둔다 — 상태 조회(M3)·취소(M4) 전용 조회 메서드는 해당 마일스톤에서
 * 추가한다(course.CourseRepository와 동일한 얇은 스타일).
 */
public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
}
