package com.hongseob.openclass_ap.member.jwt;

import com.hongseob.openclass_ap.common.config.JwtProperties;
import com.hongseob.openclass_ap.member.Member;
import jakarta.servlet.FilterChain;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * JwtAuthenticationFilter 단위 테스트 — Spring 컨텍스트 없이 필터의 SecurityContext
 * 설정 책임만 검증한다. 401/403 판정 자체는 SecurityConfig + 예외 처리 계층이
 * 수행하므로(AC-AUTH-010 ~ 013는 AuthorizationIntegrationTest가 담당), 이 테스트는
 * "유효한 토큰이면 인증이 설정되고, 그렇지 않으면 설정되지 않은 채 체인이 계속
 * 진행된다"는 필터 자체의 계약만 확인한다.
 */
class JwtAuthenticationFilterTest {

    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String SECRET = "unit-test-jwt-secret-at-least-32-bytes-long-AAAA";

    private final JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(new JwtProperties(SECRET, TTL));
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

    @BeforeEach
    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_SecurityContext에_이메일과_ROLE_권한이_설정된다() throws Exception {
        // Given
        Member member = Member.createMember("a@example.com", "hash", "Alice");
        String token = jwtTokenProvider.generateToken(member);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("a@example.com");
        assertThat(authentication.getAuthorities())
                .extracting(Object::toString)
                .containsExactly("ROLE_MEMBER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 위조된_토큰이면_인증이_설정되지_않고_체인은_계속_진행된다() throws Exception {
        // Given
        Member member = Member.createMember("a@example.com", "hash", "Alice");
        String token = jwtTokenProvider.generateToken(member);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tampered);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void Authorization_헤더가_없으면_인증이_설정되지_않고_체인은_계속_진행된다() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // sync-auditor 발견 F1 — "Bearer " 뒤에 토큰이 없으면(빈 문자열) jjwt가
    // JwtException이 아닌 IllegalArgumentException을 던져 필터 밖으로 예외가
    // 전파된다(500). MockMvc는 실제 서블릿 컨테이너와 달리 헤더 값의 후행
    // 공백(OWS)을 트리밍하지 않으므로 "Bearer "(끝에 공백) 헤더가 그대로
    // resolveToken에 전달되어 재현 가능하다.
    @Test
    void Bearer_뒤에_토큰이_없으면_예외가_전파되지_않고_인증만_설정되지_않는다() throws Exception {
        // Given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        // When
        filter.doFilter(request, response, filterChain);

        // Then
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
