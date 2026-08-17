// @MX:ANCHOR: [AUTO] 폴링 스케줄 계산 + 종단 판정 — plan.md §C.3 / design.md
// §A.4의 표를 그대로 구현한다. useRequestStatus.ts(TanStack Query
// refetchInterval)와 pollingDecision.ts가 모두 이 모듈을 소비한다.
// @MX:REASON: design.md §A.2 — 종단 판정은 화이트리스트가 아니라 `PENDING`의
// 여집합이어야 한다(REQ-ENR-004, INV-FE-002). 판정 로직이 여러 지점에
// 흩어지면 한쪽만 화이트리스트로 회귀할 위험이 있어 단일 지점에 모은다.

/** 경과 시간(ms) 구간별 다음 폴링 간격(ms). 상한 초과 시 `'stop'`. */
export type PollingIntervalDecision = number | 'stop'

/**
 * 경과 시간(ms)으로부터 다음 폴링 간격을 계산한다(REQ-ENR-006, REQ-ENR-007).
 *
 * | 경과 구간 | 간격 |
 * |---|---|
 * | [0, 5s) | 1s |
 * | [5s, 15s) | 2s |
 * | [15s, 30s) | 3s |
 * | [30s, ∞) | 중단 |
 *
 * 경계는 반개구간이다 — 정확히 30초 시점부터 중단이 지시된다(AC-FE-073a).
 */
export function computePollingInterval(elapsedMs: number): PollingIntervalDecision {
  if (elapsedMs < 5_000) {
    return 1_000
  }
  if (elapsedMs < 15_000) {
    return 2_000
  }
  if (elapsedMs < 30_000) {
    return 3_000
  }
  return 'stop'
}

/**
 * 종단 판정 — `PENDING`이 아닌 모든 값은 종단이다(REQ-ENR-004, INV-FE-002).
 * 알려진 8종 목록과 대조하는 화이트리스트 방식은 사용하지 않는다 — 백엔드가
 * 결과값을 추가해도(미지 문자열) 이 함수는 자동으로 종단을 반환하여 폴링이
 * 구조적으로 멈춘다.
 */
export function isTerminalStatus(status: string): boolean {
  return status !== 'PENDING'
}
