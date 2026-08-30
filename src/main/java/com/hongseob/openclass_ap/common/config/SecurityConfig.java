package com.hongseob.openclass_ap.common.config;

import com.hongseob.openclass_ap.member.jwt.JwtAuthenticationFilter;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * JWT 기반 무상태 인증·인가 설정 (plan.md §F M3 / §D — STATELESS, 경로 인가,
 * 401/403 구분). {@link PasswordEncoderConfig}의 {@code PasswordEncoder} Bean을
 * 재사용하며 여기서 다시 정의하지 않는다(M1 인수인계 항목).
 *
 * <p>CSRF는 비활성화한다 — 쿠키 기반 자격 증명을 사용하지 않고 모든 인증은
 * {@code Authorization} 헤더의 Bearer 토큰으로만 이뤄지므로 CSRF 공격 표면 자체가
 * 없다(plan.md §C.3 근거). 세션 생성 정책은 STATELESS로 고정한다(REQ-AUTHZ-005).</p>
 *
 * <p>인증 실패(401)와 인가 실패(403)를 명시적으로 구분하기 위해 기본
 * {@code AuthenticationEntryPoint}/{@code AccessDeniedHandler}에 의존하지 않고
 * 직접 지정한다 — formLogin/httpBasic이 비활성화된 상태의 기본 진입점은 프레임워크
 * 버전에 따라 403을 반환할 수 있어 AC-AUTH-010(무토큰 401)과 충돌할 수 있다.</p>
 *
 * <p>CORS(SPEC-FRONTEND-001 research.md §2 DEP-1) — {@code Authorization} 헤더를
 * 실은 크로스오리진 요청은 사전 요청(preflight {@code OPTIONS})을 유발하는데,
 * {@code anyRequest().authenticated()}가 이를 그대로 401로 거부하고 있었다.
 * {@code .cors(...)}로 등록되는 {@code CorsFilter}는 Spring Security의 인가
 * 필터보다 앞서 실행되며 유효한 preflight 요청에는 필터 체인을 더 진행시키지
 * 않고 자체적으로 응답하므로, 인증 여부와 무관하게 preflight를 통과시킨다.</p>
 *
 * <p><b>{@code /error}(SPEC-AUTH-001 in-place amendment, 2026-08-30)</b> — 서블릿
 * 컨테이너는 {@code DefaultHandlerExceptionResolver}가 해석한 4xx(예: {@code @Valid}
 * 실패, 파싱 불가 JSON)를 클라이언트로 돌려주기 전에 내부적으로 {@code /error}로
 * 다시 포워드한다. {@code /error}가 {@code permitAll} 목록에 없으면 이 포워드된
 * 요청 자체가 미인증 상태로 {@code anyRequest().authenticated()}에 걸려, 실제
 * 400 응답이 도달하기 전에 {@code authenticationEntryPoint}가 먼저 401(빈 본문)을
 * 반환한다 — 완전히 공개된 엔드포인트(예: {@code /api/auth/signup})에서도 발생하는
 * 결함이었다. {@code MockMvc}는 이 서블릿 내부 포워드를 재현하지 않으므로 기존
 * MockMvc 테스트는 이 결함을 검출하지 못했다({@code SecurityErrorForwardIntegrationTest}
 * 참고 — 실제 내장 서버(RANDOM_PORT)로 재현·회귀 방지한다).</p>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtTokenProvider jwtTokenProvider, CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/courses", "/api/courses/*").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, authException) ->
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                        .accessDeniedHandler((request, response, accessDeniedException) ->
                                response.sendError(HttpServletResponse.SC_FORBIDDEN)))
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * 허용 Origin은 {@link CorsProperties}(환경변수 {@code app.cors.allowed-origins})로만
     * 주입되며 소스 코드에 하드코딩하지 않는다. 이 API는 쿠키가 아닌
     * {@code Authorization} 헤더의 Bearer 토큰만 사용하므로 {@code allowCredentials}는
     * false로 고정한다 — 자격증명 기반 CORS를 도입하지 않는다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(CorsProperties corsProperties) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(corsProperties.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
