// @MX:NOTE: [AUTO] 요청 상태(폴링) 화면 — design.md §A.6 `/requests/:requestId`.
// REQ-ENR-002(202 직후 "접수됨/처리 중" 표현)·007(상한 도달 시 수동 재확인
// 수단, 식별자 유실 없음)·008(WAITLISTED 대기 순번)·009(8종+미지 문구)를
// 이 화면 하나가 배선한다. 폴링 스케줄 자체는 useRequestStatus.ts(+
// pollingDecision.ts)가 소유하며, 이 컴포넌트는 그 결과를 렌더링만 한다.

import { useEffect, useState } from 'react'
import { useRequestStatus } from './useRequestStatus'
import { loadReceiptTimestamp, saveReceiptTimestamp } from './receiptStorage'
import { computePollingInterval, isTerminalStatus } from './pollingSchedule'
import { selectTerminalMessage } from './messages'
import { ApiError } from '../api/client'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Alert } from '@/components/ui/alert'

export interface RequestStatusPageProps {
  requestId: number
  /** 세션 토큰(REQ-ENR-003의 인증(본인) 엔드포인트 호출에 필요). */
  token: string
}

export function RequestStatusPage({ requestId, token }: RequestStatusPageProps) {
  // 접수 시각은 requestId와 같은 수명(sessionStorage)에서 복원한다
  // (REQ-ENR-011) — 이 화면의 마운트 시각을 기준으로 삼지 않는다. 저장된 값이
  // 없는 경우(세션이 다른 곳에서 시작된 URL 직접 진입 등 예외적 경로)는
  // "지금"을 접수 시각으로 대체 기록한다 — 잔여 위험은 progress.md에 기록.
  const [receivedAtMs] = useState<number>(() => {
    const persisted = loadReceiptTimestamp(window.sessionStorage, requestId)
    if (persisted !== null) {
      return persisted
    }
    const fallbackNow = Date.now()
    saveReceiptTimestamp(window.sessionStorage, requestId, fallbackNow)
    return fallbackNow
  })

  const query = useRequestStatus(requestId, receivedAtMs, token)

  // 렌더 시점마다 "지금"을 다시 평가한다 — 상한(30초) 도달 여부는 매 폴링
  // 응답마다 자연히 재평가되지만(useRequestStatus의 refetchInterval), 마지막
  // 자동 폴링 이후에도 화면이 상한 도달 사실을 반영할 수 있도록 상태로도
  // 들고 있는다.
  const [nowTick, setNowTick] = useState(() => Date.now())
  useEffect(() => {
    setNowTick(Date.now())
  }, [query.dataUpdatedAt, query.errorUpdatedAt])

  const status = query.data?.status
  const elapsedMs = nowTick - receivedAtMs
  const scheduleAtCutoff = computePollingInterval(elapsedMs) === 'stop'
  const isTerminal = status !== undefined && isTerminalStatus(status)
  const autoPollingHalted = !isTerminal && scheduleAtCutoff

  if (query.isPending) {
    return <p className="text-sm text-neutral-500">불러오는 중…</p>
  }

  return (
    <section className="flex flex-col gap-3">
      <Card className="flex flex-col gap-3">
        <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">수강신청 처리 현황</h2>
        <p className="text-sm text-neutral-600 dark:text-neutral-400">요청 번호: {requestId}</p>

        {status === undefined && query.isError && (
          <Alert role="alert" tone="error">
            {query.error instanceof ApiError ? query.error.normalized.message : '상태를 불러오지 못했습니다.'}
          </Alert>
        )}

        {status === 'PENDING' && (
          // REQ-ENR-002 — 202 직후에도, 폴링 중에도 "확정"을 뜻하는 표현을
          // 쓰지 않는다. "접수됨 / 처리 중" 계열만 사용한다.
          <Alert role="status" tone="info">접수됨 — 처리 중입니다. 잠시만 기다려 주세요.</Alert>
        )}

        {isTerminal && status !== undefined && (
          <Alert role="status" tone="info">{selectTerminalMessage(status)}</Alert>
        )}

        {status === 'WAITLISTED' && query.data?.waitlistPosition != null && (
          // REQ-ENR-008 — 대기 순번을 함께 표시한다.
          <p className="text-sm text-neutral-600 dark:text-neutral-400">대기 순번: {query.data.waitlistPosition}번째</p>
        )}

        {autoPollingHalted && (
          // REQ-ENR-007 — 상한 도달 시 자동 폴링을 중단하고 수동 재확인 수단을
          // 제공한다. requestId는 계속 화면에 남아 있으므로 유실되지 않는다.
          <div className="flex flex-col gap-2">
            <Alert role="status" tone="info">
              처리가 예상보다 오래 걸리고 있습니다. 자동 확인이 중단되었습니다 — 아래 버튼으로 다시 확인해 주세요.
            </Alert>
            <Button type="button" className="w-fit" onClick={() => void query.refetch()}>
              다시 확인
            </Button>
          </div>
        )}
      </Card>
    </section>
  )
}
