package com.hongseob.openclass_ap.member.fixture;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AC-AUTH-010 ~ 014 검증 전용 테스트 픽스처 (plan.md §C.5.1).
 *
 * <p><b>테스트 소스 트리 전용 — src/main 반입 금지</b>(plan.md §C.5.1 / §G 안티패턴).
 * {@code @TestConfiguration}으로 등록되며, 이 픽스처를 {@code @Import}한 테스트
 * 클래스의 컨텍스트에만 포함된다 — {@code @TestConfiguration}은 Spring Boot 테스트
 * 컴포넌트 스캔에서 자동 제외되므로, 이 클래스는 명시적으로 {@code @Import}한
 * 테스트에서만 활성화되고 다른 통합 테스트 컨텍스트에는 노출되지 않는다.
 * {@code @RestController}를 이 클래스에 직접 붙인 것도 그 때문이다 — 별도의 중첩
 * {@code @RestController} 클래스를 두면 컴포넌트 스캔(자동)과 명시적 {@code @Bean}
 * 등록(수동)이 겹쳐 동일 경로가 두 번 매핑되는 충돌이 발생한다.</p>
 *
 * <p>두 경로 모두 고정 응답만 반환하고 도메인 로직·DB 접근을 포함하지 않는다 —
 * 401/403 판정은 전적으로 프로덕션 {@code SecurityConfig}가 수행하며, 이 클래스는
 * 인가 규칙을 절대 수정하지 않는다(plan.md §G 안티패턴).</p>
 */
@TestConfiguration
@RestController
public class AuthTestFixtureController {

    private static final AtomicInteger PROTECTED_CALL_COUNT = new AtomicInteger(0);

    /** AC-AUTH-011 — 위조·만료 토큰이 필터/인가 계층에서 차단되면 이 카운터는 증가하지 않는다. */
    public static int protectedCallCount() {
        return PROTECTED_CALL_COUNT.get();
    }

    public static void resetProtectedCallCount() {
        PROTECTED_CALL_COUNT.set(0);
    }

    @GetMapping("/api/test/protected")
    public ResponseEntity<String> protectedEndpoint() {
        PROTECTED_CALL_COUNT.incrementAndGet();
        return ResponseEntity.ok("protected-ok");
    }

    @GetMapping("/api/admin/test-ping")
    public ResponseEntity<String> adminPing() {
        return ResponseEntity.ok("admin-ok");
    }
}
