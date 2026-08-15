package com.hongseob.openclass_ap.common.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 서명·수명 설정 (plan.md §C.2). {@code app.jwt.secret}은 프로퍼티로만 주입되며
 * 소스 코드에 리터럴로 존재하지 않는다(REQ-LOGIN-004 / AC-AUTH-009) — 실제 값은
 * {@code application.properties}의 {@code ${JWT_SECRET}} 환경변수 참조 또는 테스트
 * 전용 프로퍼티 파일에서만 주입된다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl) {
}
