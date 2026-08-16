package com.hongseob.openclass_ap.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 최초 관리자 계정 시더 설정 (plan.md §C.4). {@code app.admin.password}는
 * {@link JwtProperties}와 동일한 외부화 패턴을 따라 프로퍼티(환경변수)로만
 * 주입되며 소스 코드에 리터럴로 존재하지 않는다(AC-AUTH-016).
 */
@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(String email, String password) {
}
