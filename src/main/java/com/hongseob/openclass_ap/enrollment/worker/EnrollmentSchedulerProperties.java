package com.hongseob.openclass_ap.enrollment.worker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 워커 스케줄러 설정값 (REQ-WRK-015, AC-ENR-022). 기본값은 design.md §6의
 * 산출표와 일치한다 — 폴링 주기 200ms, 배치 크기 200건(이론 처리량
 * 1,000건/초, 보수적 처리량 500건/초 가정). 실측치가 이 값과 어긋나면
 * 요구사항의 숫자를 실측에 맞게 개정한다(실측 우선 원칙, plan.md §C.6) —
 * 그 개정 근거는 progress.md §E.2에 기록한다.
 *
 * <p>{@code schedulerEnabled}는 테스트 프로파일에서 {@code false}로
 * 오버라이드되어 {@link EnrollmentQueueWorker#poll()}을 무동작시킨다
 * (research.md §5 "테스트에서 스케줄러가 자동 동작" — 타이밍 의존 테스트를
 * 배제하기 위해 테스트는 {@link EnrollmentQueueWorker#drainQueue()}를
 * 명시적으로 호출한다).</p>
 */
@ConfigurationProperties(prefix = "app.enrollment.worker")
public record EnrollmentSchedulerProperties(
        @DefaultValue("true") boolean schedulerEnabled,
        @DefaultValue("200") long pollingDelayMs,
        @DefaultValue("200") int batchSize) {
}
