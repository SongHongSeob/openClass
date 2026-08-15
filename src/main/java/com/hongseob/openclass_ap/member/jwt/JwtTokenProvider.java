package com.hongseob.openclass_ap.member.jwt;

import com.hongseob.openclass_ap.common.config.JwtProperties;
import com.hongseob.openclass_ap.member.Member;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * 액세스 토큰 발급·검증 (plan.md §C.2 — HMAC-SHA256, 단일 단기 토큰, 무효화 없음).
 * 서명 비밀키는 {@link JwtProperties}를 통해 프로퍼티로만 주입된다 — 이 클래스에는
 * 비밀키 리터럴이 존재하지 않는다(REQ-LOGIN-004 / AC-AUTH-009).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey key;
    private final Duration accessTokenTtl;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenTtl = jwtProperties.accessTokenTtl();
    }

    /**
     * sub(이메일)·role·iat·exp 클레임을 담은 액세스 토큰을 발급한다(AC-AUTH-007).
     * 비밀번호 평문·해시는 어떤 클레임에도 포함하지 않는다.
     */
    public String generateToken(Member member) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(accessTokenTtl);
        return Jwts.builder()
                .subject(member.getEmail())
                .claim("role", member.getRole().name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰을 검증하고 클레임을 반환한다. 서명이 다른 비밀키로 검증되거나 만료된
     * 토큰은 {@link io.jsonwebtoken.JwtException}(그 하위 타입)을 던진다.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
