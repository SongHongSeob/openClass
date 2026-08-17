package com.hongseob.openclass_ap.common.config;

import com.hongseob.openclass_ap.member.fixture.AuthTestFixtureController;
import com.hongseob.openclass_ap.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;

/**
 * CORS preflight 통합 테스트 (SPEC-FRONTEND-001 research.md §2 DEP-1).
 *
 * <p>이 SPEC 이전에는 {@code SecurityConfig}에 {@code .cors(...)} 호출이 없어
 * {@code Authorization} 헤더가 유발하는 사전 요청(preflight {@code OPTIONS})이
 * {@code anyRequest().authenticated()}에 의해 그대로 401로 거부되었다 — 인가
 * 티어(permitAll/authenticated)와 무관하게 브라우저에서 호출한 모든 엔드포인트가
 * 차단되는 결함이었다. 이 테스트는 {@code GET /api/courses}(permitAll 티어)와
 * {@code GET /api/test/protected}(authenticated 티어) 각각에 대해 preflight가
 * 200/204로 통과하고 올바른 {@code Access-Control-Allow-Origin}이 응답되는지,
 * 그리고 허용 목록에 없는 Origin은 그 헤더를 되돌려 받지 않는지를 검증한다.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AuthTestFixtureController.class)
class CorsIntegrationTest extends AbstractIntegrationTest {

    /** application.properties의 app.cors.allowed-origins 기본값과 동일하다. */
    private static final String ALLOWED_ORIGIN = "http://localhost:5173";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permitAll_엔드포인트에_대한_preflight는_허용_Origin으로_통과한다() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/courses")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(200, 204);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
    }

    @Test
    void authenticated_엔드포인트에_대한_preflight도_인증_없이_허용_Origin으로_통과한다() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/test/protected")
                        .header("Origin", ALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isIn(200, 204);
        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isEqualTo(ALLOWED_ORIGIN);
        // AC-AUTH-010(무토큰 401)과의 경계 확인 — 401/403이 아니어야 preflight가 통과한 것이다.
        assertThat(result.getResponse().getStatus()).isNotIn(401, 403);
    }

    @Test
    void 허용_목록에_없는_Origin은_Access_Control_Allow_Origin_헤더를_되돌려받지_못한다() throws Exception {
        MvcResult result = mockMvc.perform(options("/api/courses")
                        .header("Origin", DISALLOWED_ORIGIN)
                        .header("Access-Control-Request-Method", "GET"))
                .andReturn();

        assertThat(result.getResponse().getHeader("Access-Control-Allow-Origin")).isNull();
    }
}
