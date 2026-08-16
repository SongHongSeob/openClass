package com.hongseob.openclass_ap.course;

import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 강좌 엔티티 및 스키마 통합 테스트 — AC-CRS-001 ~ AC-CRS-005 (M1).
 *
 * <p>AC-CRS-002/003/005는 애플리케이션 계층을 우회한 {@link JdbcTemplate} 직접
 * INSERT/UPDATE로 DB CHECK 제약의 실제 동작을 검증한다 (acceptance.md 검증 방법).
 * 실제 PostgreSQL(Testcontainers)에서만 의미가 있다 — H2는 CHECK 제약 위반 시
 * 동작이 달라 대체 금지다 (plan.md §D).</p>
 *
 * <p><b>M1 범위 주석</b>: AC-CRS-004(i)의 "생성·수정·마감·삭제·조회 API를 각각
 * 1회씩 호출" 중 수정·마감·삭제·조회 API는 M2/M3에서 만들어지므로 이 마일스톤에서는
 * 검증할 수 없다 — 이 클래스는 생성 경로만 검증한다 (PASS-WITH-DEBT, M3/M4에서
 * 전체 API 호출 시나리오로 재검증). AC-CRS-004(ii)의 정적 검색은
 * {@link CourseEnrolledCountMutationAbsenceTest}가 별도로 전담한다. AC-CRS-005(i)의
 * 애플리케이션 API 경로 검증도 관리자 API가 없는 M1에서는 생략하고 M3에서
 * 재검증한다 — 이 클래스는 (ii) DB 계층만 검증한다.</p>
 */
@SpringBootTest
class CourseSchemaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUp() {
        courseRepository.deleteAll();
    }

    private Course newCourse(String title, int capacity) {
        return Course.create(title, "설명", capacity,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30));
    }

    private void insertRawCourse(String title, int capacity, int enrolledCount, String status) {
        jdbcTemplate.update(
                "INSERT INTO course (title, description, capacity, enrolled_count, starts_at, ends_at, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                title, "설명", capacity, enrolledCount,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30), status, LocalDateTime.now());
    }

    private Long findIdByTitle(String title) {
        return jdbcTemplate.queryForObject("SELECT id FROM course WHERE title = ?", Long.class, title);
    }

    // AC-CRS-001 — 강좌 필수 속성 보유
    @Test
    void 강좌를_생성하면_설명을_제외한_모든_필수_컬럼이_NULL이_아니다() {
        Course saved = courseRepository.save(newCourse("스프링 기초", 10));

        Course found = courseRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getId()).isNotNull();
        assertThat(found.getTitle()).isNotNull();
        assertThat(found.getCapacity()).isNotNull();
        assertThat(found.getEnrolledCount()).isNotNull();
        assertThat(found.getStartsAt()).isNotNull();
        assertThat(found.getEndsAt()).isNotNull();
        assertThat(found.getStatus()).isNotNull();
        assertThat(found.getCreatedAt()).isNotNull();
    }

    // AC-CRS-002 — 정원 1 미만 DB 제약 거부 (경계: 1은 허용)
    @Test
    void 정원이_1_미만이면_DB_제약이_거부하고_1이면_허용한다() {
        assertThatThrownBy(() -> insertRawCourse("정원0강좌", 0, 0, "OPEN"))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThatCode(() -> insertRawCourse("정원1강좌", 1, 0, "OPEN"))
                .doesNotThrowAnyException();
    }

    // AC-CRS-003 — 확정 인원 CHECK 제약 (0 <= enrolled_count <= capacity)
    @Test
    void 확정_인원이_정원을_초과하거나_음수이면_DB_제약이_거부하고_값이_유지된다() {
        insertRawCourse("정원5확정5강좌", 5, 5, "OPEN");
        Long id = findIdByTitle("정원5확정5강좌");

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE course SET enrolled_count = ? WHERE id = ?", 6, id))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE course SET enrolled_count = ? WHERE id = ?", -1, id))
                .isInstanceOf(DataIntegrityViolationException.class);

        Integer enrolledCount = jdbcTemplate.queryForObject(
                "SELECT enrolled_count FROM course WHERE id = ?", Integer.class, id);
        assertThat(enrolledCount).isEqualTo(5);
    }

    // AC-CRS-004(i) 일부 — 강좌 생성 경로는 확정 인원을 항상 0으로만 시작한다
    // (수정·마감·삭제·조회 API 호출 검증은 M2/M3에서 재검증 — 클래스 주석 참조)
    @Test
    void 강좌_생성_경로로_만든_모든_강좌의_확정_인원은_0이다() {
        courseRepository.save(newCourse("확정0확인강좌1", 10));
        courseRepository.save(newCourse("확정0확인강좌2", 5));

        assertThat(courseRepository.findAll())
                .allSatisfy(course -> assertThat(course.getEnrolledCount()).isZero());
    }

    // AC-CRS-005(ii) — 모집 상태 DB 제약 (OPEN/CLOSED 외 값 거부)
    // (i) 애플리케이션 API 경로 검증은 관리자 API가 없는 M1에서는 생략, M3에서 재검증
    @Test
    void 모집_상태가_OPEN_CLOSED가_아니면_DB_제약이_거부하고_값이_유지된다() {
        assertThatThrownBy(() -> insertRawCourse("잘못된상태강좌", 10, 0, "ARCHIVED"))
                .isInstanceOf(DataIntegrityViolationException.class);

        insertRawCourse("정상상태강좌", 10, 0, "OPEN");
        Long id = findIdByTitle("정상상태강좌");

        assertThatThrownBy(() -> jdbcTemplate.update("UPDATE course SET status = ? WHERE id = ?", "ARCHIVED", id))
                .isInstanceOf(DataIntegrityViolationException.class);

        String status = jdbcTemplate.queryForObject("SELECT status FROM course WHERE id = ?", String.class, id);
        assertThat(status).isEqualTo("OPEN");
    }
}
