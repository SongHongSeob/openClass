package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 로그인 API 전체 스택 통합 테스트 (HTTP → 컨트롤러 → 서비스 → 실제 PostgreSQL).
 * AC-AUTH-007, AC-AUTH-008을 검증하고, M1에서 PASS-WITH-DEBT로 남겨졌던
 * AC-AUTH-006의 "정규화된 이메일로 로그인 성공" 절을 완결시킨다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class LoginIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    private void signup(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","password":"%s","name":"Alice"}
                                """.formatted(email, password)))
                .andExpect(status().isCreated());
    }

    private String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    // AC-AUTH-007 — 로그인 성공 및 토큰 클레임
    @Test
    void 로그인_성공시_200과_함께_클레임이_모두_담긴_토큰이_반환된다() throws Exception {
        signup("a@example.com", "password1");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson("a@example.com", "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        Claims claims = jwtTokenProvider.parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo("a@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();

        String payload = new String(java.util.Base64.getUrlDecoder().decode(token.split("\\.")[1]));
        assertThat(payload).doesNotContain("password1");
    }

    // AC-AUTH-008 — 로그인 실패 구별 불가
    @Test
    void 틀린_비밀번호와_미가입_이메일의_로그인_실패_응답은_바이트_단위로_동일하다() throws Exception {
        signup("a@example.com", "password1");

        MvcResult wrongPassword = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson("a@example.com", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        MvcResult unknownEmail = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson("zzz@example.com", "any-password")))
                .andExpect(status().isUnauthorized())
                .andReturn();

        assertThat(wrongPassword.getResponse().getStatus())
                .isEqualTo(unknownEmail.getResponse().getStatus());
        assertThat(wrongPassword.getResponse().getContentAsByteArray())
                .isEqualTo(unknownEmail.getResponse().getContentAsByteArray());
    }

    // AC-AUTH-006 upgrade — M1 PASS-WITH-DEBT 완결 (정규화된 이메일로 로그인 성공)
    @Test
    void 대소문자와_공백이_포함된_이메일로_가입해도_정규화된_이메일로_로그인_성공한다() throws Exception {
        signup("  A@Example.COM  ", "password1");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(loginJson("a@example.com", "password1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }
}
