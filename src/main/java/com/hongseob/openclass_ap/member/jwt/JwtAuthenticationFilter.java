package com.hongseob.openclass_ap.member.jwt;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <token>} 헤더를 검증해 {@link SecurityContextHolder}에
 * 인증 정보를 설정한다 (plan.md §F M3 — REQ-AUTHZ-001 ~ REQ-AUTHZ-003).
 *
 * <p>토큰이 없거나 위조·만료되어 검증에 실패하면 이 필터는 인증을 설정하지 않고
 * 체인을 그대로 진행시킨다 — 401/403 판정은 이 필터가 아니라
 * {@link com.hongseob.openclass_ap.common.config.SecurityConfig}의 인가 규칙과
 * Spring Security의 예외 처리 계층이 수행한다. 세션을 전혀 사용하지 않으므로
 * {@code HttpSession}에는 어떤 상태도 기록하지 않는다(REQ-AUTHZ-005, AC-AUTH-014).</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_PREFIX = "ROLE_";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            authenticate(token);
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String token) {
        try {
            var claims = jwtTokenProvider.parseClaims(token);
            String email = claims.getSubject();
            String role = claims.get("role", String.class);
            Authentication authentication = new UsernamePasswordAuthenticationToken(
                    email, null, List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException e) {
            SecurityContextHolder.clearContext();
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
