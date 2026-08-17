// @MX:NOTE: [AUTO] 강좌 목록 화면(REQ-CAT-001~003·006). 세션 없이도 열람
// 가능하다 — 이 컴포넌트는 세션을 전혀 참조하지 않는다(REQ-CAT-006). 페이지
// 이동은 백엔드 메타데이터 기반이며(REQ-CAT-002), 자체 분할하지 않는다.

import { useEffect, useState } from 'react'
import { getCourses } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { Course } from '../api/types'
import { computePageControls } from './catalogModel'

const PAGE_SIZE = 10

export interface CourseListPageProps {
  /** REQ-CAT-004 — 항목 선택 시 상세 화면으로 전환한다. */
  onSelectCourse: (courseId: number) => void
}

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; items: Course[] }

export function CourseListPage({ onSelectCourse }: CourseListPageProps) {
  const [page, setPage] = useState(0)
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  // 목록 조회는 항상 요청 시점의 페이지 메타데이터를 그대로 보존한다
  // (REQ-CAT-002) — 화면이 totalElements/totalPages/currentPage를 재계산하지
  // 않는다.
  const [controls, setControls] = useState(() =>
    computePageControls({ items: [], totalElements: 0, totalPages: 0, currentPage: 0 }),
  )

  useEffect(() => {
    let cancelled = false
    setState({ status: 'loading' })
    getCourses(page, PAGE_SIZE)
      .then((coursePage) => {
        if (cancelled) return
        setControls(computePageControls(coursePage))
        setState({ status: 'loaded', items: coursePage.items })
      })
      .catch((error: unknown) => {
        if (cancelled) return
        const message = error instanceof ApiError ? error.normalized.message : '강좌 목록을 불러오지 못했습니다.'
        setState({ status: 'error', message })
      })
    return () => {
      cancelled = true
    }
  }, [page])

  return (
    <section>
      <h2>강좌 목록</h2>
      {state.status === 'loading' && <p>불러오는 중…</p>}
      {state.status === 'error' && <p role="alert">{state.message}</p>}
      {state.status === 'loaded' && controls.isEmpty && <p>표시할 강좌가 없습니다.</p>}
      {state.status === 'loaded' && !controls.isEmpty && (
        <ul>
          {state.items.map((course) => (
            <li key={course.id}>
              <button type="button" onClick={() => onSelectCourse(course.id)}>
                {course.title}
              </button>
              {' — '}
              정원 {course.capacity} · 확정 {course.enrolledCount} · 잔여 {course.remainingCapacity} · {course.status}
            </li>
          ))}
        </ul>
      )}
      {state.status === 'loaded' && !controls.isEmpty && (
        <p>
          <button type="button" disabled={!controls.hasPrevious} onClick={() => setPage((current) => current - 1)}>
            이전
          </button>
          {' '}
          {controls.currentPage + 1} / {Math.max(controls.totalPages, 1)}
          {' '}
          <button type="button" disabled={!controls.hasNext} onClick={() => setPage((current) => current + 1)}>
            다음
          </button>
        </p>
      )}
    </section>
  )
}
