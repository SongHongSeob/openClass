package com.hongseob.openclass_ap.course;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link Course} 저장소. M2/M3의 카탈로그·관리자 조회 메서드는 해당 마일스톤에서
 * 추가한다 — M1은 스키마·제약 검증에 필요한 최소 형태만 둔다
 * (member.MemberRepository와 동일한 얇은 스타일, plan.md §C.4.1-3).
 *
 * <p>{@link #findByTitleContainingIgnoreCase}는 SPEC-COURSE-001 Amendment
 * 1(REQ-CAT-007)에서 추가된 강좌명 부분 일치 검색 파생 쿼리다 — 대소문자를
 * 무시하고 강좌명에 부분 일치하는 강좌만 페이지네이션하여 반환한다.</p>
 */
public interface CourseRepository extends JpaRepository<Course, Long> {

    Page<Course> findByTitleContainingIgnoreCase(String keyword, Pageable pageable);
}
