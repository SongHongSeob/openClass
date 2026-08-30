// @MX:ANCHOR: [AUTO] TanStack Query `refetchInterval`에 배선되는 순수 판정
// 로직 — REQ-ENR-010의 비대칭(네트워크 오류는 다음 주기 재시도 / 401은
// 재시도 없이 세션 폐기로 위임)을 하나의 함수로 고정한다.
// useRequestStatus.ts는 이 함수의 결과를 그대로 `refetchInterval`에 반환한다.
// @MX:REASON: design.md §A.4 — "TanStack Query의 retry 옵션은 기본적으로
// 실패를 재시도하는데, 401은 재시도하지 않고 세션 폐기로 넘겨야 한다." 판정을
// 훅 본체에 인라인하면 React/TanStack 렌더 없이 테스트할 수 없다 — 순수
// 함수로 분리해 pollingSchedule.test.ts와 동일하게 vitest(node 환경)에서
// 직접 검증한다(plan.md §D.1 — 화면 렌더링 테스트는 필수 범위가 아니다).

import type { ErrorClassification } from '../api/errors'
import { computePollingInterval, isTerminalStatus } from './pollingSchedule'

export interface PollingDecisionInput {
  /** 최근 성공 응답의 상태값. 아직 성공 응답이 없으면 undefined. */
  status: string | undefined
  /** 최근 실패의 정규화 분류(errors.ts). 실패가 없으면 undefined. */
  errorClassification: ErrorClassification | undefined
  /** 접수 시각(receiptStorage.ts에 보존된 값)으로부터의 경과 시간(ms). */
  elapsedMs: number
}

/**
 * 다음 폴링까지의 간격(ms)을 반환한다. `false`는 "더 이상 자동 폴링하지
 * 않는다"를 뜻한다 — TanStack Query `refetchInterval`의 반환 계약과 동일하다.
 *
 * 판정 순서 (REQ-ENR-004·005·010):
 * 1. 401(session-expired) — 재시도하지 않는다. `client.ts`의 전역 구독이
 *    이미 세션 폐기를 촉발했으므로 이 함수는 폴링만 멈춘다.
 * 2. 종단 상태 도달 — 더 이상 조회하지 않는다(INV-FE-002).
 * 3. 그 외(성공 없음 · 네트워크 오류 포함) — 접수 시각 기준 경과 스케줄을
 *    그대로 따른다. 네트워크 오류는 "다음 예정 주기에 재시도"로 자연히
 *    귀결된다 — 별도의 재시도 카운터를 두지 않는다.
 */
export function decideNextPoll(input: PollingDecisionInput): number | false {
  if (input.errorClassification === 'session-expired') {
    return false
  }

  if (input.status !== undefined && isTerminalStatus(input.status)) {
    return false
  }

  const interval = computePollingInterval(input.elapsedMs)
  return interval === 'stop' ? false : interval
}
