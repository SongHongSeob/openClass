package com.hongseob.openclass_ap.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * 실제 PostgreSQL 인스턴스가 필요한 통합 테스트의 공통 베이스 클래스.
 * (SPEC-AUTH-001 plan.md §D — 유니크 제약 동작은 H2가 아닌 실제 PostgreSQL로 검증해야 한다.)
 *
 * <p>컨테이너는 테스트 JVM당 1회(static 필드) 기동되며, 연결 정보는
 * {@code @ServiceConnection}으로 주입된다. 이는 어떤 {@code spring.datasource.*}
 * 프로퍼티보다 우선하므로 활성 프로파일과 무관하게 동작하고, 개발자의 로컬
 * Supabase 자격 증명을 건드리지 않는다.</p>
 */
@Testcontainers
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // SPEC-ENROLLMENT-001 plan.md §C.1 — schema.sql의 부분 유니크 인덱스가
        // ddl-auto 이후에, Testcontainers(비-임베디드) DB에서도 실행되어야 한다.
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always",
        // SPEC-ENROLLMENT-001 M2, research.md §5 — 테스트는 @Scheduled 자동
        // 폴링에 의존하면 타이밍에 종속된다. 모든 통합 테스트는 워커를
        // 명시적으로 drainQueue()로 구동한다.
        "app.enrollment.worker.scheduler-enabled=false"
})
public abstract class AbstractIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
