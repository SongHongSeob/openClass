// @MX:NOTE: [AUTO] 강좌 상세 화면(REQ-CAT-004~006). 세션 없이도 열람
// 가능하다(REQ-CAT-006). CLOSED 상태에서는 수강신청 조작을 제공하지 않고
// 마감 상태를 표시한다(REQ-CAT-005). M4 — 인증된 회원에게 수강신청 조작을
// 노출한다(REQ-ENR-001). 접수(202) 성공 시 접수 시각을 requestId와 함께
// 보존하고(REQ-ENR-011) `/requests/:requestId`로 이동한다 — 이 화면 자체는
// 아직 라우트로 전환하지 않았지만(과업 지시 B1), BrowserRouter가 트리
// 최상단에 있으므로 `useNavigate`는 이 위치에서도 정상 동작한다.

import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import { getCourseDetail, submitEnrollment } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { Course } from '../api/types'
import { isEnrollmentBlocked } from './catalogModel'
import { saveReceiptTimestamp } from '../enrollment/receiptStorage'
import { useSession } from '../session/useSession'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Alert } from '@/components/ui/alert'

export interface CourseDetailPageProps {
  courseId: number
  /** 목록 화면으로 되돌아간다. 라우터를 도입하지 않은 콜백 기반 전환(과업 지시 B1). */
  onBack: () => void
}

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; course: Course }

export function CourseDetailPage({ courseId, onBack }: CourseDetailPageProps) {
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const { session } = useSession()
  const navigate = useNavigate()
  const [enrolling, setEnrolling] = useState(false)
  const [enrollError, setEnrollError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setState({ status: 'loading' })
    getCourseDetail(courseId)
      .then((course) => {
        if (!cancelled) setState({ status: 'loaded', course })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        // AC-FE-046 — 존재하지 않는 강좌 식별자(404)에도 화면이 중단되지
        // 않는다. errors.ts 단일 정규화 지점의 결과만 사용한다(REQ-ERR-002).
        const message = error instanceof ApiError ? error.normalized.message : '강좌 정보를 불러오지 못했습니다.'
        setState({ status: 'error', message })
      })
    return () => {
      cancelled = true
    }
  }, [courseId])

  async function handleEnroll() {
    if (session.status !== 'authenticated') {
      return
    }
    setEnrolling(true)
    setEnrollError(null)
    try {
      // REQ-ENR-001 — 접수 성공(202) 시 요청 식별자를 확보한다.
      const receipt = await submitEnrollment(courseId, session.token)
      // REQ-ENR-011 — 폴링 상한 측정의 기준점(접수 시각)을 requestId와 같은
      // 수명(sessionStorage)에 즉시 보존한다 — 화면 마운트 시각이 아니다.
      saveReceiptTimestamp(window.sessionStorage, receipt.requestId, Date.now())
      navigate(`/requests/${receipt.requestId}`)
    } catch (error) {
      const message = error instanceof ApiError ? error.normalized.message : '신청 처리 중 오류가 발생했습니다.'
      setEnrollError(message)
    } finally {
      setEnrolling(false)
    }
  }

  return (
    <section className="flex flex-col gap-4">
      <Button type="button" variant="ghost" className="h-auto w-fit px-0" onClick={onBack}>
        ← 목록으로
      </Button>
      {state.status === 'loading' && <p className="text-sm text-neutral-500">불러오는 중…</p>}
      {state.status === 'error' && <Alert role="alert" tone="error">{state.message}</Alert>}
      {state.status === 'loaded' && (
        <Card className="flex flex-col gap-3">
          <h2 className="text-xl font-semibold text-neutral-900 dark:text-neutral-100">{state.course.title}</h2>
          <p className="text-neutral-700 dark:text-neutral-300">{state.course.description}</p>
          <p className="text-sm text-neutral-600 dark:text-neutral-400">
            정원 {state.course.capacity} · 확정 인원 {state.course.enrolledCount} · 잔여 정원{' '}
            {state.course.remainingCapacity}
          </p>
          <p className="text-sm text-neutral-600 dark:text-neutral-400">
            모집 기간 {state.course.startsAt} ~ {state.course.endsAt}
          </p>
          {isEnrollmentBlocked(state.course.status) ? (
            <Alert role="status" tone="info">마감된 강좌입니다. 신청을 받지 않습니다.</Alert>
          ) : session.status === 'authenticated' ? (
            <>
              <Button type="button" onClick={() => void handleEnroll()} disabled={enrolling} className="w-fit">
                {enrolling ? '접수 중…' : '수강신청'}
              </Button>
              {enrollError !== null && <Alert role="alert" tone="error">{enrollError}</Alert>}
            </>
          ) : (
            // REQ-SES-009 — 인증이 필요한 조작은 세션 없이 진입을 허용하지
            // 않는다. 상세 열람 자체는 공개이므로(REQ-CAT-006) 화면 렌더링을
            // 막지 않고, 신청 조작만 로그인 안내로 대체한다.
            <Alert role="status" tone="info">로그인 후 신청할 수 있습니다.</Alert>
          )}
        </Card>
      )}
    </section>
  )
}
