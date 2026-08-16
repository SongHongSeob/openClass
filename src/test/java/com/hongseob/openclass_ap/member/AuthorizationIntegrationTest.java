package com.hongseob.openclass_ap.member;

import com.hongseob.openclass_ap.common.config.JwtProperties;
import com.hongseob.openclass_ap.member.fixture.AuthTestFixtureController;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * JwtAuthenticationFilter + SecurityConfig 인가 통합 테스트 (plan.md §F M3).
 * plan.md §C.5.1이 선언한 {@link AuthTestFixtureController} 픽스처의 두 경로를
 * 호출하여 AC-AUTH-010 ~ 015를 검증한다. 401/403 판정은 전적으로
 * {@code SecurityConfig}가 수행하며, 이 테스트는 그 판정 결과만 관찰한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestFixtureController.class)
class AuthorizationIntegrationTest extends AbstractIntegrationTest {

    private static final String PROTECTED_PATH = "/api/test/protected";
    private static final String ADMIN_PATH = "/api/admin/test-ping";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        memberRepository.deleteAll();
        AuthTestFixtureController.resetProtectedCallCount();
    }

    /** 실제 발급 로직(JwtTokenProvider)과 무관하게 임의 role 클레임의 토큰을 직접 구성한다. */
    private String rawToken(String email, String role, Instant expiration) {
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Instant issuedAt = Instant.now();
        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    private String validToken(String email, String role) {
        return rawToken(email, role, Instant.now().plus(jwtProperties.accessTokenTtl()));
    }

    private String tamperSignature(String token) {
        int lastDot = token.lastIndexOf('.');
        String signature = token.substring(lastDot + 1);
        char lastChar = signature.charAt(signature.length() - 1);
        char replacement = lastChar == 'A' ? 'B' : 'A';
        String tamperedSignature = signature.substring(0, signature.length() - 1) + replacement;
        return token.substring(0, lastDot + 1) + tamperedSignature;
    }

    // AC-AUTH-010 — 토큰 없음 차단
    @Test
    void 토큰없이_보호_엔드포인트를_호출하면_401이_반환된다() throws Exception {
        mockMvc.perform(get(PROTECTED_PATH))
                .andExpect(status().isUnauthorized());
    }

    // AC-AUTH-011 — 위조·만료 토큰 차단 (두 케이스 모두 401 + 핸들러 미도달)
    @Test
    void 위조되거나_만료된_토큰이면_401이_반환되고_핸들러에_도달하지_않는다() throws Exception {
        String valid = validToken("a@example.com", "MEMBER");
        String forged = tamperSignature(valid);
        String expired = rawToken("a@example.com", "MEMBER", Instant.now().minusSeconds(1));

        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + expired))
                .andExpect(status().isUnauthorized());

        assertThat(AuthTestFixtureController.protectedCallCount()).isZero();
    }

    // AC-AUTH-012 — MEMBER의 관리자 엔드포인트 접근 차단 (403, 401 아님)
    @Test
    void MEMBER_역할_토큰으로_관리자_엔드포인트를_호출하면_403이_반환된다() throws Exception {
        String token = validToken("member@example.com", "MEMBER");

        mockMvc.perform(get(ADMIN_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // AC-AUTH-013 — ADMIN의 관리자 엔드포인트 접근 허용
    @Test
    void ADMIN_역할_토큰으로_관리자_엔드포인트를_호출하면_200이_반환된다() throws Exception {
        String token = validToken("admin@example.com", "ADMIN");

        mockMvc.perform(get(ADMIN_PATH).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // AC-AUTH-014 — 무상태 검증 (동일 토큰 20회 호출, 매번 성공 + 세션 쿠키 없음)
    @Test
    void 동일_토큰으로_20회_호출해도_전부_성공하고_세션_쿠키가_생기지_않는다() throws Exception {
        String token = validToken("stateless@example.com", "MEMBER");

        for (int i = 0; i < 20; i++) {
            MvcResult result = mockMvc.perform(get(PROTECTED_PATH).header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andReturn();
            // sync-auditor 발견 F4 — MockHttpServletResponse는 서블릿 컨테이너의
            // Set-Cookie 발급 로직을 실행하지 않으므로 헤더 검사는 공허한 단정이다.
            // 대신 요청 처리 중 실제로 HttpSession이 생성됐는지(getSession(false))를
            // 관찰한다 — STATELESS 정책이면 세션이 아예 만들어지지 않는다.
            String setCookie = result.getResponse().getHeader("Set-Cookie");
            assertThat(setCookie).satisfiesAnyOf(
                    header -> assertThat(header).isNull(),
                    header -> assertThat(header).doesNotContain("JSESSIONID")
            );
            assertThat(result.getRequest().getSession(false)).isNull();
        }
    }

    // AC-AUTH-015 — 공개 엔드포인트 무토큰 접근 (회원가입·로그인은 401이 아니다)
    @Test
    void 공개_엔드포인트는_토큰없이_호출해도_401이_아니다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"email":"authz-public@example.com","password":"password1","name":"Alice"}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {"email":"authz-public@example.com","password":"password1"}
                                """))
                .andExpect(status().isOk());
    }
}
