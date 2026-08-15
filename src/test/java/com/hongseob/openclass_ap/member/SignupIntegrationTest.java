package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 회원가입 API 전체 스택 통합 테스트 (HTTP → 컨트롤러 → 서비스 → 실제 PostgreSQL).
 * AC-AUTH-001 ~ AC-AUTH-006(부분)을 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SignupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @BeforeEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    private String signupJson(String email, String password, String extraJson) {
        String base = """
                {"email":"%s","password":"%s","name":"Alice"%s}
                """.formatted(email, password, extraJson);
        return base.trim();
    }

    // AC-AUTH-001 — 회원가입 성공
    @Test
    void 회원가입_성공시_201과_MEMBER_역할의_회원이_생성된다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("a@example.com", "password1", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("a@example.com"))
                .andExpect(jsonPath("$.role").value("MEMBER"));

        assertThat(memberRepository.count()).isEqualTo(1);
        Member saved = memberRepository.findByEmail("a@example.com").orElseThrow();
        assertThat(saved.getRole()).isEqualTo(MemberRole.MEMBER);
        assertThat(saved.getPasswordHash()).isNotEqualTo("password1");
        assertThat(saved.getPasswordHash()).matches("^\\$2[aby]\\$.*");
    }

    // AC-AUTH-002 — 중복 이메일 가입 거부
    @Test
    void 중복_이메일_가입은_409를_반환하고_행이_증가하지_않는다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("dup@example.com", "password1", "")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("dup@example.com", "password2", "")))
                .andExpect(status().isConflict());

        assertThat(memberRepository.count()).isEqualTo(1);
    }

    // AC-AUTH-003 — 입력 검증 실패
    @Test
    void 잘못된_이메일_형식이면_400을_반환하고_회원을_생성하지_않는다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("not-an-email", "password1", "")))
                .andExpect(status().isBadRequest());

        assertThat(memberRepository.count()).isZero();
    }

    @Test
    void 비밀번호가_8자_미만이면_400을_반환하고_회원을_생성하지_않는다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("short@example.com", "short1", "")))
                .andExpect(status().isBadRequest());

        assertThat(memberRepository.count()).isZero();
    }

    // AC-AUTH-004 — 역할 주입 차단
    @Test
    void 요청_본문에_role_ADMIN을_포함해도_MEMBER로만_생성된다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("inject@example.com", "password1", ",\"role\":\"ADMIN\"")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("MEMBER"));

        assertThat(memberRepository.findAll())
                .allSatisfy(member -> assertThat(member.getRole()).isEqualTo(MemberRole.MEMBER));
    }

    // AC-AUTH-005 — 저장소 평문 부재
    @Test
    void 평문_비밀번호는_어떤_컬럼에도_저장되지_않는다() throws Exception {
        String plainPassword = "PlainSecret123";
        for (String email : new String[] {"m1@example.com", "m2@example.com", "m3@example.com"}) {
            mockMvc.perform(post("/api/auth/signup")
                            .contentType("application/json")
                            .content(signupJson(email, plainPassword, "")))
                    .andExpect(status().isCreated());
        }

        var all = memberRepository.findAll();
        assertThat(all).hasSize(3);
        for (Member member : all) {
            assertThat(member.getEmail()).doesNotContain(plainPassword);
            assertThat(member.getName()).doesNotContain(plainPassword);
            assertThat(member.getPasswordHash()).doesNotContain(plainPassword);
        }
    }

    // AC-AUTH-006 — 이메일 정규화 (PASS-WITH-DEBT: 로그인 성공 검증은 M2 이후로 이관 — 아래 클래스 주석 참조)
    @Test
    void 대소문자와_공백이_포함된_이메일은_트림_소문자로_정규화되어_저장된다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("  A@Example.COM  ", "password1", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("a@example.com"));

        Member saved = memberRepository.findByEmail("a@example.com").orElseThrow();
        assertThat(saved.getEmail()).isEqualTo("a@example.com");

        // 정규화된 이메일로 재가입 시도하면 중복으로 거부되고 행 수는 1건으로 유지된다.
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content(signupJson("a@example.com", "password2", "")))
                .andExpect(status().isConflict());

        assertThat(memberRepository.count()).isEqualTo(1);
    }
}
