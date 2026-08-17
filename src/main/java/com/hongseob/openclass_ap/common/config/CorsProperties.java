package com.hongseob.openclass_ap.common.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * CORS 허용 Origin 목록 설정 (SPEC-FRONTEND-001 research.md §2 DEP-1).
 * {@link JwtProperties}/{@link AdminProperties}와 동일한 외부화 패턴을 따라
 * 프로퍼티(환경변수)로만 주입되며 소스 코드에 오리진을 하드코딩하지 않는다 —
 * 환경별로 다른 프론트엔드 배포 오리진을 재컴파일 없이 교체할 수 있어야 한다.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
