package com.hongseob.openclass_ap.enrollment.worker.fixture;

import com.hongseob.openclass_ap.enrollment.worker.EnrollmentFailureInjector;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * AC-ENR-016/017 검증 전용 테스트 픽스처 — "확정 행 INSERT 이후, 커밋
 * 이전" 예외를 결정적으로 재현한다. {@code @TestConfiguration}이므로 이
 * 클래스를 명시적으로 {@code @Import}한 테스트 컨텍스트에만 등록되고 다른
 * 통합 테스트에는 노출되지 않는다({@code member.fixture.AuthTestFixtureController}와
 * 동일한 패턴, plan.md §C.5.1 계보).
 */
@TestConfiguration
public class EnrollmentFailureInjectorTestConfig {

    @Bean
    @Primary
    public EnrollmentFailureInjector enrollmentFailureInjector() {
        return new ControllableFailureInjector();
    }

    /**
     * {@link #failNextFor(Long)}로 지정한 요청 id에 한해서만 확정 INSERT
     * 직후 예외를 던진다. 나머지 요청은 아무 영향도 받지 않는다 — 큐 내
     * 다른 요청이 정상적으로 종단 결과에 도달하는지(AC-ENR-016)를 함께
     * 검증할 수 있어야 하기 때문이다.
     */
    public static final class ControllableFailureInjector implements EnrollmentFailureInjector {

        private final AtomicReference<Long> failingRequestId = new AtomicReference<>();

        public void failNextFor(Long requestId) {
            failingRequestId.set(requestId);
        }

        public void reset() {
            failingRequestId.set(null);
        }

        @Override
        public void afterEnrollmentPersisted(Long requestId) {
            if (requestId.equals(failingRequestId.get())) {
                throw new IllegalStateException(
                        "AC-ENR-016/017 테스트 전용 강제 실패 주입: requestId=" + requestId);
            }
        }
    }
}
