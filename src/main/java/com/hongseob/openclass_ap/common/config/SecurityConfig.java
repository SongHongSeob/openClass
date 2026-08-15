package com.hongseob.openclass_ap.common.config;

import com.hongseob.openclass_ap.member.jwt.JwtAuthenticationFilter;
import com.hongseob.openclass_ap.member.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

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
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenProvider jwtTokenProvider)
            throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/courses").permitAll()
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
}
