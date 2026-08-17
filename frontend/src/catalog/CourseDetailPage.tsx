// @MX:NOTE: [AUTO] 강좌 상세 화면(REQ-CAT-004~006). 세션 없이도 열람
// 가능하다(REQ-CAT-006). CLOSED 상태에서는 수강신청 조작을 제공하지 않고
// 마감 상태를 표시한다(REQ-CAT-005) — 이 마일스톤에는 아직 신청 조작 자체가
// 없으므로(M4가 배선) 마감 표시만 담당하되, `isEnrollmentBlocked`를 M4가
// 재사용할 수 있도록 노출한다.

import { useEffect, useState } from 'react'
import { getCourseDetail } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { Course } from '../api/types'
import { isEnrollmentBlocked } from './catalogModel'

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

  return (
    <section>
      <button type="button" onClick={onBack}>
        ← 목록으로
      </button>
      {state.status === 'loading' && <p>불러오는 중…</p>}
      {state.status === 'error' && <p role="alert">{state.message}</p>}
      {state.status === 'loaded' && (
        <>
          <h2>{state.course.title}</h2>
          <p>{state.course.description}</p>
          <p>
            정원 {state.course.capacity} · 확정 인원 {state.course.enrolledCount} · 잔여 정원{' '}
            {state.course.remainingCapacity}
          </p>
          <p>
            모집 기간 {state.course.startsAt} ~ {state.course.endsAt}
          </p>
          {isEnrollmentBlocked(state.course.status) ? (
            <p role="status">마감된 강좌입니다. 신청을 받지 않습니다.</p>
          ) : (
            <p role="status">모집 상태: {state.course.status}</p>
          )}
        </>
      )}
    </section>
  )
}
