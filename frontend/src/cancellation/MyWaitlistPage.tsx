// @MX:NOTE: [AUTO] 내 대기명단 목록 화면(REQ-CNL-003~010). 대기명단 취소는
// 200 동기 응답이며 확정 취소와 달리 큐를 경유하지 않는다 — 폴링을 개시하지
// 않고(REQ-CNL-003), 취소 성공 시 목록을 재조회한다(REQ-CNL-009,
// `decidePostCancelAction`).

import { useCallback, useEffect, useState } from 'react'
import { cancelWaitlistEntry, getMyWaitlistEntries } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { WaitlistEntryId, WaitlistListItem } from '../api/types'
import {
  decidePostCancelAction,
  describeCancelError,
  formatWaitlistPositionLabel,
  resolveWaitlistCancelTarget,
  toListView,
} from './cancellationModel'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Alert } from '@/components/ui/alert'

export interface MyWaitlistPageProps {
  token: string
}

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; items: WaitlistListItem[] }

export function MyWaitlistPage({ token }: MyWaitlistPageProps) {
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [cancellingId, setCancellingId] = useState<WaitlistEntryId | null>(null)
  const [cancelError, setCancelError] = useState<string | null>(null)

  const load = useCallback(() => {
    setState({ status: 'loading' })
    getMyWaitlistEntries(token)
      .then((items) => {
        setState({ status: 'loaded', items })
      })
      .catch((error: unknown) => {
        const message = error instanceof ApiError ? error.normalized.message : '내 대기명단 목록을 불러오지 못했습니다.'
        setState({ status: 'error', message })
      })
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  // REQ-CNL-003 — 200 동기 응답이므로 폴링을 개시하지 않는다. REQ-CNL-009 —
  // 취소 성공 시 항상 재조회하며(decidePostCancelAction), 로컬에서 항목을
  // 제거하지 않는다.
  async function handleCancel(item: WaitlistListItem) {
    setCancelError(null)
    const targetId = resolveWaitlistCancelTarget(item)
    setCancellingId(targetId)
    try {
      await cancelWaitlistEntry(targetId, token)
      if (decidePostCancelAction() === 'refetch') {
        load()
      }
    } catch (error) {
      setCancelError(describeCancelError(error))
    } finally {
      setCancellingId(null)
    }
  }

  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">내 대기명단</h2>
      {cancelError !== null && <Alert role="alert" tone="error">{cancelError}</Alert>}
      {state.status === 'loading' && <p className="text-sm text-neutral-500">불러오는 중…</p>}
      {state.status === 'error' && <Alert role="alert" tone="error">{state.message}</Alert>}
      {state.status === 'loaded' &&
        (() => {
          const view = toListView(state.items)
          if (view.status === 'empty') {
            // REQ-CNL-007 — 0건은 오류가 아니라 "보유 내역 없음"이다.
            return <p className="text-sm text-neutral-500">보유 내역 없음 — 아직 대기 중인 강좌가 없습니다.</p>
          }
          return (
            <ul className="flex flex-col gap-3">
              {/* REQ-CNL-008 — 응답 순서를 그대로 표시한다(재정렬 금지). */}
              {view.items.map((item) => (
                <li key={item.waitlistEntryId}>
                  <Card className="flex flex-wrap items-center justify-between gap-3 p-4">
                    {/* REQ-CNL-010 / INV-FE-011 — position은 courseTitle과 나란히
                        표시하는 단일 문구로만 렌더링한다(단독 표시 금지). */}
                    <span className="text-sm text-neutral-700 dark:text-neutral-300">
                      {formatWaitlistPositionLabel(item)}
                    </span>
                    <Button
                      type="button"
                      variant="destructive"
                      size="sm"
                      disabled={cancellingId === item.waitlistEntryId}
                      onClick={() => void handleCancel(item)}
                    >
                      취소
                    </Button>
                  </Card>
                </li>
              ))}
            </ul>
          )
        })()}
    </section>
  )
}
