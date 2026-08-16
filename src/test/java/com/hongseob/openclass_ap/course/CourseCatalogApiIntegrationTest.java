package com.hongseob.openclass_ap.course;

import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 공개 강좌 카탈로그 API 통합 테스트 — AC-CAT-001 ~ AC-CAT-005 (M2).
 *
 * <p>모든 요청은 {@code Authorization} 헤더 없이 호출한다 — D1 결함(상세 조회가
 * 비인증으로 열려 있는지)을 실제로 관찰하려면 무헤더 전제가 필수다(plan.md
 * §C.2.1, acceptance.md AC-CAT-002 근거).</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CourseCatalogApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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

    private Long insertRawCourse(String title, int capacity, int enrolledCount, String status) {
        jdbcTemplate.update(
                "INSERT INTO course (title, description, capacity, enrolled_count, starts_at, ends_at, status, created_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                title, "설명", capacity, enrolledCount,
                LocalDateTime.now().plusDays(1), LocalDateTime.now().plusDays(30), status, LocalDateTime.now());
        return jdbcTemplate.queryForObject("SELECT id FROM course WHERE title = ?", Long.class, title);
    }

    // AC-CAT-001 — 비인증 목록 조회 및 페이지 분할 경계
    @Test
    void 비인증으로_목록을_조회하면_전체가_반환되고_size로_페이지가_실제로_분할된다() throws Exception {
        Course c1 = courseRepository.save(newCourse("강좌A", 10));
        Course c2 = courseRepository.save(newCourse("강좌B", 10));
        Course c3 = courseRepository.save(newCourse("강좌C", 10));
        Set<Long> allIds = Set.of(c1.getId(), c2.getId(), c3.getId());

        // (i) 기본 파라미터 — 3건 전부, 메타데이터 포함
        String defaultBody = mockMvc.perform(get("/api/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[0].id").exists())
                .andExpect(jsonPath("$.items[0].title").exists())
                .andExpect(jsonPath("$.items[0].capacity").exists())
                .andExpect(jsonPath("$.items[0].enrolledCount").exists())
                .andExpect(jsonPath("$.items[0].remainingCapacity").exists())
                .andExpect(jsonPath("$.items[0].startsAt").exists())
                .andExpect(jsonPath("$.items[0].endsAt").exists())
                .andExpect(jsonPath("$.items[0].status").exists())
                .andReturn().getResponse().getContentAsString();
        assertThat(defaultBody).isNotBlank();

        // (ii) size=2 — 1페이지 정확히 2건, 2페이지 정확히 1건, 합집합이 전체와 일치
        String page1Body = mockMvc.perform(get("/api/courses").param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andReturn().getResponse().getContentAsString();
        String page2Body = mockMvc.perform(get("/api/courses").param("size", "2").param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andReturn().getResponse().getContentAsString();

        List<Integer> page1Ids = JsonPath.read(page1Body, "$.items[*].id");
        List<Integer> page2Ids = JsonPath.read(page2Body, "$.items[*].id");
        Set<Long> unionIds = java.util.stream.Stream.concat(page1Ids.stream(), page2Ids.stream())
                .map(Integer::longValue)
                .collect(Collectors.toSet());

        assertThat(page1Ids).hasSize(2);
        assertThat(page2Ids).hasSize(1);
        assertThat(unionIds).isEqualTo(allIds);
    }

    // AC-CAT-002 — 비인증 상세 조회 및 잔여 정원 계산
    @Test
    void 비인증으로_상세를_조회하면_200과_잔여_정원이_반환되고_저장_컬럼은_없다() throws Exception {
        Long id = insertRawCourse("정원10확정4강좌", 10, 4, "OPEN");

        mockMvc.perform(get("/api/courses/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remainingCapacity").value(6))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.enrolledCount").value(4));

        List<String> columnNames = jdbcTemplate.queryForList(
                "SELECT column_name FROM information_schema.columns WHERE table_name = 'course'", String.class);
        assertThat(columnNames).noneMatch(name -> name.toLowerCase().contains("remaining"));
    }

    // AC-CAT-003 — 존재하지 않는 강좌 조회
    @Test
    void 존재하지_않는_강좌를_조회하면_404가_반환된다() throws Exception {
        mockMvc.perform(get("/api/courses/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("COURSE_NOT_FOUND"));
    }

    // AC-CAT-004 — 조회의 무부작용성 (목록·상세 20회 반복 호출)
    @Test
    void 목록과_상세_조회를_20회_반복해도_course_테이블이_변하지_않는다() throws Exception {
        Course c1 = courseRepository.save(newCourse("불변확인강좌1", 10));
        courseRepository.save(newCourse("불변확인강좌2", 5));
        courseRepository.save(newCourse("불변확인강좌3", 20));

        List<Map<String, Object>> before = jdbcTemplate.queryForList("SELECT * FROM course ORDER BY id");

        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/api/courses")).andExpect(status().isOk());
            mockMvc.perform(get("/api/courses/" + c1.getId())).andExpect(status().isOk());
        }

        List<Map<String, Object>> after = jdbcTemplate.queryForList("SELECT * FROM course ORDER BY id");
        assertThat(after).isEqualTo(before);
    }

    // AC-CAT-005 — 마감 강좌 노출
    @Test
    void 마감_강좌도_목록에서_필터링되지_않고_상태가_CLOSED로_표시된다() throws Exception {
        courseRepository.save(newCourse("공개강좌1", 10));
        courseRepository.save(newCourse("공개강좌2", 10));
        insertRawCourse("마감강좌", 10, 0, "CLOSED");

        String body = mockMvc.perform(get("/api/courses").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(3))
                .andReturn().getResponse().getContentAsString();

        List<String> closedStatuses = JsonPath.read(body, "$.items[?(@.title == '마감강좌')].status");
        assertThat(closedStatuses).containsExactly("CLOSED");
    }
}
