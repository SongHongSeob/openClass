// @MX:NOTE: [AUTO] 내 수강신청 목록 화면(REQ-CNL-001·002·004~009). 확정 취소는
// 202를 경유하므로 M4가 만든 폴링 경로(`/requests/:requestId`,
// `enrollment/useRequestStatus.ts`)를 그대로 재사용한다 — 취소 응답의
// `requestId`로 접수 시각을 보존하고 그 경로로 이동하면, "접수됨/처리 중" 표시
// (REQ-CNL-002)와 종단 문구("취소가 완료되었습니다" — `enrollment/messages.ts`의
// `CANCELLED`)가 이미 배선되어 있어 새로 만들 필요가 없다.

import { useCallback, useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { cancelEnrollment, getMyEnrollments } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { EnrollmentListItem } from '../api/types'
import { saveReceiptTimestamp } from '../enrollment/receiptStorage'
import { describeCancelError, resolveEnrollmentCancelTarget, toListView } from './cancellationModel'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Alert } from '@/components/ui/alert'

export interface MyEnrollmentsPageProps {
  token: string
}

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; items: EnrollmentListItem[] }

export function MyEnrollmentsPage({ token }: MyEnrollmentsPageProps) {
  const navigate = useNavigate()
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [cancellingId, setCancellingId] = useState<number | null>(null)
  const [cancelError, setCancelError] = useState<string | null>(null)

  const load = useCallback(() => {
    setState({ status: 'loading' })
    getMyEnrollments(token)
      .then((items) => {
        setState({ status: 'loaded', items })
      })
      .catch((error: unknown) => {
        const message = error instanceof ApiError ? error.normalized.message : '내 수강신청 목록을 불러오지 못했습니다.'
        setState({ status: 'error', message })
      })
  }, [token])

  useEffect(() => {
    load()
  }, [load])

  // REQ-CNL-001 — 취소 호출 후 응답의 requestId로 폴링 경로(/requests/:id)로
  // 이동한다. REQ-CNL-009 — 이 화면으로 다시 돌아오면 위 useEffect가 새로
  // 마운트되어 재조회한다. 로컬에서 항목을 제거하지 않는다.
  async function handleCancel(item: EnrollmentListItem) {
    setCancelError(null)
    const targetId = resolveEnrollmentCancelTarget(item)
    setCancellingId(targetId)
    try {
      const receipt = await cancelEnrollment(targetId, token)
      saveReceiptTimestamp(window.sessionStorage, receipt.requestId, Date.now())
      navigate(`/requests/${receipt.requestId}`)
    } catch (error) {
      setCancelError(describeCancelError(error))
    } finally {
      setCancellingId(null)
    }
  }

  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">내 수강신청</h2>
      {cancelError !== null && <Alert role="alert" tone="error">{cancelError}</Alert>}
      {state.status === 'loading' && <p className="text-sm text-neutral-500">불러오는 중…</p>}
      {state.status === 'error' && <Alert role="alert" tone="error">{state.message}</Alert>}
      {state.status === 'loaded' &&
        (() => {
          const view = toListView(state.items)
          if (view.status === 'empty') {
            // REQ-CNL-007 — 0건은 오류가 아니라 "보유 내역 없음"이다. 신규
            // 회원의 첫 진입이 항상 이 상태다.
            return <p className="text-sm text-neutral-500">보유 내역 없음 — 아직 신청한 수강신청이 없습니다.</p>
          }
          return (
            <ul className="flex flex-col gap-3">
              {/* REQ-CNL-008 — 응답 순서를 그대로 표시한다(재정렬 금지). */}
              {view.items.map((item) => (
                <li key={item.enrollmentId}>
                  <Card className="flex flex-wrap items-center justify-between gap-3 p-4">
                    <span className="text-sm text-neutral-700 dark:text-neutral-300">
                      {item.courseTitle} — {item.status} · 신청일 {item.enrolledAt}
                    </span>
                    <Button
                      type="button"
                      variant="destructive"
                      size="sm"
                      disabled={cancellingId === item.enrollmentId}
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
