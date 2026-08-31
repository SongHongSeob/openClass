package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 관리자 회원 관리 API 통합 테스트. {@code /api/admin/**} 경로 인가는
 * {@code SecurityConfig}가 이미 처리하므로 이 테스트는 그 결과(403)만 관찰한다
 * — {@code CourseAdminApiIntegrationTest}와 동일한 패턴.
 */
@SpringBootTest
@AutoConfigureMockMvc
class MemberAdminApiIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private Member admin;
    private Member member;
    private String adminToken;
    private String memberToken;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        admin = memberRepository.save(Member.createAdmin("admin@example.com", "hash", "관리자"));
        member = memberRepository.save(Member.createMember("member@example.com", "hash", "회원"));
        adminToken = jwtTokenProvider.generateToken(admin);
        memberToken = jwtTokenProvider.generateToken(member);
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    private String roleBody(String role) {
        return "{\"role\":\"%s\"}".formatted(role);
    }

    // (a) ADMIN이 MEMBER를 ADMIN으로 승격
    @Test
    void ADMIN이_MEMBER를_승격하면_200과_변경된_역할이_반환된다() throws Exception {
        mockMvc.perform(patch("/api/admin/members/" + member.getId() + "/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(roleBody("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    // (b) ADMIN이 다른 ADMIN을 MEMBER로 강등
    @Test
    void ADMIN이_다른_ADMIN을_강등하면_200과_변경된_역할이_반환된다() throws Exception {
        Member otherAdmin = memberRepository.save(Member.createAdmin("other-admin@example.com", "hash", "관리자2"));

        mockMvc.perform(patch("/api/admin/members/" + otherAdmin.getId() + "/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(roleBody("MEMBER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        Member reloaded = memberRepository.findById(otherAdmin.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(MemberRole.MEMBER);
    }

    // (c) 존재하지 않는 대상 식별자 → 404
    @Test
    void 존재하지_않는_회원_식별자로_역할을_변경하면_404가_반환된다() throws Exception {
        mockMvc.perform(patch("/api/admin/members/999999/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(roleBody("ADMIN")))
                .andExpect(status().isNotFound());
    }

    // (d) ADMIN이 자기 자신의 역할을 변경 시도 → 409, DB 무변경
    @Test
    void ADMIN이_자기_자신의_역할을_변경하려_하면_409이고_역할이_변하지_않는다() throws Exception {
        mockMvc.perform(patch("/api/admin/members/" + admin.getId() + "/role")
                        .header("Authorization", bearer(adminToken))
                        .contentType("application/json")
                        .content(roleBody("MEMBER")))
                .andExpect(status().isConflict());

        Member reloaded = memberRepository.findById(admin.getId()).orElseThrow();
        assertThat(reloaded.getRole()).isEqualTo(MemberRole.ADMIN);
    }

    // (e) MEMBER 역할 토큰으로 접근 시 403 (목록 조회 + 역할 변경 둘 다)
    @Test
    void MEMBER_역할_토큰으로_관리자_회원_API를_호출하면_403이다() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(memberToken)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/admin/members/" + member.getId() + "/role")
                        .header("Authorization", bearer(memberToken))
                        .contentType("application/json")
                        .content(roleBody("ADMIN")))
                .andExpect(status().isForbidden());
    }

    // (f) 목록 응답 바디에 passwordHash가 포함되지 않는다
    @Test
    void 회원_목록_응답에는_passwordHash가_포함되지_않는다() throws Exception {
        mockMvc.perform(get("/api/admin/members")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[*].password_hash").doesNotExist());
    }
}
