package com.hongseob.openclass_ap.course;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link Course} 저장소. M2/M3의 카탈로그·관리자 조회 메서드는 해당 마일스톤에서
 * 추가한다 — M1은 스키마·제약 검증에 필요한 최소 형태만 둔다
 * (member.MemberRepository와 동일한 얇은 스타일, plan.md §C.4.1-3).
 */
public interface CourseRepository extends JpaRepository<Course, Long> {
}
