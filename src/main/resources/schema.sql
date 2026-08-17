-- SPEC-ENROLLMENT-001 plan.md §C.1 / design.md §1.1 — 부분 유니크 인덱스
-- (WHERE 절 포함)는 Hibernate JPA 애노테이션으로 표현할 수 없다. ddl-auto가
-- 먼저 테이블을 생성한 뒤(spring.jpa.defer-datasource-initialization=true +
-- spring.sql.init.mode=always) 이 스크립트가 실행되어 나머지 제약을 보강한다.
-- 이 프로젝트는 별도 마이그레이션 도구가 없으므로(Course.java 클래스 주석 참고)
-- ddl-auto + 이 스크립트의 조합이 스키마 정의의 전부다. IF NOT EXISTS로 재기동 시
-- 중복 생성을 막는다.

-- INV-ENR-003 — 동일 강좌·동일 회원의 유효한 확정 수강신청은 최대 1건.
-- status='CANCELLED' 행은 이력으로 보존되어야 하므로 WHERE로 ENROLLED만 대상.
CREATE UNIQUE INDEX IF NOT EXISTS uk_enrollment_member_course_active
    ON enrollment (member_id, course_id)
    WHERE status = 'ENROLLED';

-- INV-ENR-004 — 동일 강좌 내 활성 대기 순번은 유일.
CREATE UNIQUE INDEX IF NOT EXISTS uk_waitlist_entry_course_position_active
    ON waitlist_entry (course_id, position)
    WHERE status = 'WAITING';

-- INV-ENR-009 (2차 감사 E1) — 동일 강좌·동일 회원의 활성 대기명단 항목은 최대 1건.
CREATE UNIQUE INDEX IF NOT EXISTS uk_waitlist_entry_member_course_active
    ON waitlist_entry (member_id, course_id)
    WHERE status = 'WAITING';
