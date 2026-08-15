package com.hongseob.openclass_ap.member.jwt;

import com.hongseob.openclass_ap.common.config.JwtProperties;
import com.hongseob.openclass_ap.member.Member;
import com.hongseob.openclass_ap.member.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * JwtTokenProvider 단위 테스트 — Spring 컨텍스트 없이 JwtProperties를 직접 주입하여
 * 서명·클레임·검증 실패 시나리오를 검증한다 (AC-AUTH-007, AC-AUTH-009).
 */
class JwtTokenProviderTest {

    private static final Duration TTL = Duration.ofMinutes(30);

    private JwtTokenProvider providerWithSecret(String secret) {
        return new JwtTokenProvider(new JwtProperties(secret, TTL));
    }

    // AC-AUTH-007 — 로그인 성공 및 토큰 클레임
    @Test
    void generateToken은_sub_role_iat_exp_클레임을_모두_포함한다() {
        // Given
        JwtTokenProvider provider = providerWithSecret("test-secret-key-at-least-32-bytes-long-AAAA");
        Member member = Member.createMember("a@example.com", "bcrypt-hash", "Alice");

        // When
        String token = provider.generateToken(member);
        Claims claims = provider.parseClaims(token);

        // Then
        assertThat(claims.getSubject()).isEqualTo("a@example.com");
        assertThat(claims.get("role", String.class)).isEqualTo("MEMBER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void generateToken의_페이로드_문자열에는_비밀번호_평문이나_해시가_포함되지_않는다() {
        // Given
        JwtTokenProvider provider = providerWithSecret("test-secret-key-at-least-32-bytes-long-AAAA");
        Member member = Member.createMember("a@example.com", "$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ", "Alice");

        // When
        String token = provider.generateToken(member);

        // Then — 헤더.페이로드.서명 각 세그먼트를 Base64Url 디코딩해 전체 페이로드 문자열을 검사한다.
        String[] segments = token.split("\\.");
        assertThat(segments).hasSize(3);
        String payload = new String(java.util.Base64.getUrlDecoder().decode(segments[1]));
        assertThat(payload).doesNotContain("$2a$10$abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ");
        assertThat(payload).doesNotContain("password");
    }

    // AC-AUTH-009 — 서명 비밀키 외부 주입 (다른 비밀키로 구성한 검증기는 검증에 실패한다)
    @Test
    void 다른_비밀키로_생성된_토큰은_검증에_실패한다() {
        // Given
        JwtTokenProvider providerA = providerWithSecret("secret-A-0123456789ABCDEF0123456789ABCDEF");
        JwtTokenProvider providerB = providerWithSecret("secret-B-0123456789ABCDEF0123456789ABCDEF");
        Member member = Member.createMember("a@example.com", "bcrypt-hash", "Alice");
        String token = providerA.generateToken(member);

        // When / Then
        assertThatThrownBy(() -> providerB.parseClaims(token))
                .isInstanceOf(JwtException.class);
    }
}
