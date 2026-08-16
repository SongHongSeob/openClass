package com.hongseob.openclass_ap.enrollment.worker;

/**
 * design.md §4.2가 요구하는 "확정 행 INSERT 이후, 커밋 이전" 예외 시나리오
 * (AC-ENR-016/017)를 결정적으로 재현하기 위한 테스트 전용 훅. 프로덕션 기본
 * 구현({@link NoOpEnrollmentFailureInjector})은 아무 동작도 하지 않는다 —
 * 정상 처리 흐름에는 어떤 영향도 주지 않는다.
 *
 * <p>단일 워커가 요청을 순차 처리하는 이 SPEC의 설계(REQ-WRK-012)와
 * REQ-WRK-007의 중복 검사·원자적 정원 게이트 덕분에, 확정 INSERT 이후
 * 커밋 이전에 예외가 발생하는 경합은 정상 애플리케이션 흐름만으로는
 * 결정적으로 재현되지 않는다 — 그 경합을 막는 것이 바로 이 방어선들의
 * 목적이기 때문이다. {@code EnrollmentLockProperties}(M1)가 접수 잠금
 * 메커니즘의 효과를 대조군으로 증명한 것과 동일한 이유로, 이 훅은 그
 * 방어선이 없을 때(=실패가 실제로 발생했을 때) 워커가 올바르게 복구하는지
 * (AC-ENR-016 큐 생존성, AC-ENR-017 원자성+실패 기록 보존)를 증명하기 위한
 * 테스트 전용 시드다.</p>
 */
public interface EnrollmentFailureInjector {

    void afterEnrollmentPersisted(Long requestId);
}
