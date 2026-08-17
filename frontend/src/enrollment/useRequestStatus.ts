// @MX:ANCHOR: [AUTO] §A.4 폴링 스케줄의 실행 지점(design.md §A.8) — TanStack
// Query `refetchInterval`을 pollingDecision.ts의 순수 판정에 그대로 위임한다.
// M4 대상 요구사항: REQ-ENR-003~007·010, INV-FE-002.
// @MX:REASON: plan.md §C.6 — 이 SPEC에서 사다리 4단계(신규 의존성)를
// 정당화하는 유일한 항목. `useEffect`+`setTimeout`으로 직접 구현하면 타이머
// 중복·언마운트 정리 누락 결함이 발생하기 쉬운데, 이 SPEC의 목적이 "시스템이
// 실제로 동작함을 확인"하는 것이므로 확인 도구 자체의 결함은 목적을
// 훼손한다. `refetchInterval`의 함수 형태가 "직전 결과를 보고 다음 간격
// 결정 또는 중단"을 선언적으로 표현한다(design.md §A.4).

import { useQuery, type UseQueryResult } from '@tanstack/react-query'
import { getEnrollmentRequestStatus } from '../api/endpoints'
import { ApiError } from '../api/client'
import { decideNextPoll } from './pollingDecision'
import type { RequestStatus } from '../api/types'

/**
 * `requestId` 상태를 폴링한다. `receivedAtMs`는 `receiptStorage.ts`에서 복원한
 * 접수 시각이어야 한다 — 훅 마운트 시각이 아니다(REQ-ENR-011).
 *
 * 재시도(TanStack Query 내장 `retry`)는 끈다 — 이 SPEC의 재시도 정책은
 * `refetchInterval` 스케줄 자체가 담당하며(REQ-ENR-010), 내장 `retry`를 함께
 * 켜 두면 만료된 토큰으로 불필요한 401을 중복 유발할 수 있다(design.md
 * §A.4).
 */
export function useRequestStatus(requestId: number, receivedAtMs: number, token: string): UseQueryResult<RequestStatus> {
  return useQuery({
    queryKey: ['enrollment-request-status', requestId],
    queryFn: () => getEnrollmentRequestStatus(requestId, token),
    enabled: Number.isFinite(requestId) && Number.isFinite(receivedAtMs),
    retry: false,
    refetchInterval: (query) => {
      const errorClassification = query.state.error instanceof ApiError ? query.state.error.normalized.classification : undefined
      const elapsedMs = Date.now() - receivedAtMs
      return decideNextPoll({
        status: query.state.data?.status,
        errorClassification,
        elapsedMs,
      })
    },
  })
}
