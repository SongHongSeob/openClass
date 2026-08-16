package com.hongseob.openclass_ap.course.admin;

import com.hongseob.openclass_ap.course.Course;
import com.hongseob.openclass_ap.course.CourseRepository;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRepository;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 강좌 관리 API 통합 테스트 — AC-ADM-001 ~ AC-ADM-008 (M3).
 *
 * <p>{@code /api/admin/**} 경로 인가는 {@code SecurityConfig}가 이미 처리하므로
 * 이 테스트는 그 결과(403)만 관찰한다(plan.md §C.2). ADMIN/MEMBER 토큰은 실제
 * {@link JwtTokenProvider}로 발급한다 — {@code AuthorizationIntegrationTest}와
 * 동일한 패턴이다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
class CourseAdminApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        courseRepository.deleteAll();
        memberRepository.deleteAll();
        Member admin = memberRepository.save(Member.createAdmin("admin@example.com", "hash", "관리자"));
        Member member = memberRepository.save(Member.createMember("member@example.com", "hash", "회원"));
        adminToken = jwtTokenProvider.generateToken(admin);
        memberToken = jwtTokenProvider.generateToken(member);
    }

    private String bearer(String token) {
        return "Bearer " + token;
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

    private String createBody(String title, int capacity) {
        return """
                {"title":"%s","description":"설명","capacity":%d,"startsAt":"2027-01-01T00:00:00","endsAt":"2027-01-31T00:00:00"}
                """.formatted(title, capacity);
    }

    // AC-ADM-001 — ADMIN 강좌 생성
    @Test
    void ADMIN이_강좌를_생성하면_201과_식별자가_반환되고_OPEN_확정인원0이다() throws Exception {
        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("신규강좌", 10)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.enrolledCount").value(0));

        assertThat(courseRepository.findAll()).hasSize(1);
    }

    // AC-ADM-002 — 비관리자 접근 차단 (생성·수정·마감·삭제 4종 모두 403, DB 무변경)
    @Test
    void MEMBER_역할_토큰으로_관리자_강좌_API를_호출하면_모두_403이고_DB가_변하지_않는다() throws Exception {
        Course existing = courseRepository.save(newCourse("기존강좌", 10));
        List<Map<String, Object>> before = jdbcTemplate.queryForList("SELECT * FROM course ORDER BY id");

        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content(createBody("MEMBER생성시도", 10)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/courses/" + existing.getId())
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content(createBody("MEMBER수정시도", 10)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/courses/" + existing.getId() + "/close")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/admin/courses/" + existing.getId())
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        List<Map<String, Object>> after = jdbcTemplate.queryForList("SELECT * FROM course ORDER BY id");
        assertThat(after).isEqualTo(before);
    }

    // AC-ADM-003 — 정원 1 미만 거부 (생성 + 수정)
    @Test
    void 정원이_1_미만이면_생성과_수정_모두_400이고_강좌가_변경되지_않는다() throws Exception {
        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("정원0시도", 0)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/admin/courses")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("정원음수시도", -1)))
                .andExpect(status().isBadRequest());
        assertThat(courseRepository.findAll()).isEmpty();

        Course existing = courseRepository.save(newCourse("정원수정대상", 10));
        mockMvc.perform(patch("/api/admin/courses/" + existing.getId())
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("정원수정대상", 0)))
                .andExpect(status().isBadRequest());

        Course reloaded = courseRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getCapacity()).isEqualTo(10);
    }

    // AC-ADM-004 — 확정 인원 미만 정원 축소 거부 (경계 포함, 409 단일 코드)
    @Test
    void 정원을_확정인원_미만으로_축소하면_409이고_정확히_같은_값이면_허용된다() throws Exception {
        Long id = insertRawCourse("축소대상", 10, 7, "OPEN");

        mockMvc.perform(patch("/api/admin/courses/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("축소대상", 6)))
                .andExpect(status().isConflict());

        Map<String, Object> afterReject = jdbcTemplate.queryForMap("SELECT * FROM course WHERE id = ?", id);
        assertThat(((Number) afterReject.get("capacity")).intValue()).isEqualTo(10);
        assertThat(((Number) afterReject.get("enrolled_count")).intValue()).isEqualTo(7);

        mockMvc.perform(patch("/api/admin/courses/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("축소대상", 7)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(7));

        Map<String, Object> afterAllow = jdbcTemplate.queryForMap("SELECT * FROM course WHERE id = ?", id);
        assertThat(((Number) afterAllow.get("capacity")).intValue()).isEqualTo(7);
        assertThat(((Number) afterAllow.get("enrolled_count")).intValue()).isEqualTo(7);
    }

    // AC-ADM-005 — 정원 증설 반영 + 확정 인원 불변 (승격 로직 정적 검색은 별도 테스트)
    @Test
    void 정원을_증설하면_200이고_확정인원은_변하지_않는다() throws Exception {
        Long id = insertRawCourse("증설대상", 2, 2, "OPEN");

        mockMvc.perform(patch("/api/admin/courses/" + id)
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("증설대상", 4)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(4))
                .andExpect(jsonPath("$.enrolledCount").value(2));

        Map<String, Object> after = jdbcTemplate.queryForMap("SELECT * FROM course WHERE id = ?", id);
        assertThat(((Number) after.get("enrolled_count")).intValue()).isEqualTo(2);
    }

    // AC-ADM-006 — 강좌 마감 전이
    @Test
    void ADMIN이_마감_API를_호출하면_200이고_상태가_CLOSED가_되며_행이_삭제되지_않는다() throws Exception {
        Course existing = courseRepository.save(newCourse("마감대상", 10));

        mockMvc.perform(post("/api/admin/courses/" + existing.getId() + "/close")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertThat(courseRepository.findById(existing.getId())).isPresent();
        Course reloaded = courseRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getStatus().name()).isEqualTo("CLOSED");
    }

    // AC-ADM-007 — 물리 삭제 금지 (soft delete)
    @Test
    void 삭제_API를_호출해도_행이_존재하고_상태만_CLOSED로_전이한다() throws Exception {
        Course existing = courseRepository.save(newCourse("삭제대상", 10));

        mockMvc.perform(delete("/api/admin/courses/" + existing.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        assertThat(courseRepository.findById(existing.getId())).isPresent();
        Course reloaded = courseRepository.findById(existing.getId()).orElseThrow();
        assertThat(reloaded.getStatus().name()).isEqualTo("CLOSED");
        assertThat(courseRepository.count()).isEqualTo(1);
    }

    // AC-ADM-008 — 존재하지 않는 강좌 변경 요청 (수정·마감·삭제 각각 404, DB 무변경)
    @Test
    void 존재하지_않는_강좌를_수정_마감_삭제하면_모두_404이고_DB가_변하지_않는다() throws Exception {
        List<Map<String, Object>> before = jdbcTemplate.queryForList("SELECT * FROM course ORDER BY id");

        mockMvc.perform(patch("/api/admin/courses/999999")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(createBody("존재안함", 10)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/admin/courses/999999/close")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/admin/courses/999999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound());

        List<Map<String, Object>> after = jdbcTemplate.queryForList("SELECT * FROM course ORDER BY id");
        assertThat(after).isEqualTo(before);
    }
}
