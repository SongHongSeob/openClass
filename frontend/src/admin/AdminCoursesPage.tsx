// @MX:NOTE: [AUTO] 관리자 강좌 목록(REQ-ADM-010) — 공개 카탈로그 엔드포인트
// (`GET /api/courses`)를 그대로 재사용한다. 관리자 전용 목록 엔드포인트는
// 존재하지 않는다 — 설계 선택이 아니라 사실이다(design.md §A.7).

import { useCallback, useEffect, useState } from 'react'
import { closeCourse, getCourses } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { Course } from '../api/types'
import { computePageControls } from '../catalog/catalogModel'

const PAGE_SIZE = 10

export interface AdminCoursesPageProps {
  token: string
  onCreateCourse: () => void
  onEditCourse: (courseId: number) => void
}

type LoadState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'loaded'; items: Course[] }

export function AdminCoursesPage({ token, onCreateCourse, onEditCourse }: AdminCoursesPageProps) {
  const [page, setPage] = useState(0)
  const [state, setState] = useState<LoadState>({ status: 'loading' })
  const [controls, setControls] = useState(() =>
    computePageControls({ items: [], totalElements: 0, totalPages: 0, currentPage: 0 }),
  )
  const [closingId, setClosingId] = useState<number | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const load = useCallback(() => {
    setState({ status: 'loading' })
    getCourses(page, PAGE_SIZE)
      .then((coursePage) => {
        setControls(computePageControls(coursePage))
        setState({ status: 'loaded', items: coursePage.items })
      })
      .catch((error: unknown) => {
        const message = error instanceof ApiError ? error.normalized.message : '강좌 목록을 불러오지 못했습니다.'
        setState({ status: 'error', message })
      })
  }, [page])

  useEffect(() => {
    load()
  }, [load])

  // REQ-ADM-008/009 — "삭제"가 아니라 마감. 확정자·대기자는 보존된다.
  const handleClose = async (courseId: number) => {
    setActionError(null)
    setClosingId(courseId)
    try {
      await closeCourse(courseId, token)
      load()
    } catch (error) {
      const message = error instanceof ApiError ? error.normalized.message : '강좌 마감 중 오류가 발생했습니다.'
      setActionError(message)
    } finally {
      setClosingId(null)
    }
  }

  return (
    <section>
      <h2>관리자 — 강좌 관리</h2>
      <button type="button" onClick={onCreateCourse}>
        강좌 생성
      </button>
      {actionError && <p role="alert">{actionError}</p>}
      {state.status === 'loading' && <p>불러오는 중…</p>}
      {state.status === 'error' && <p role="alert">{state.message}</p>}
      {state.status === 'loaded' && controls.isEmpty && <p>등록된 강좌가 없습니다.</p>}
      {state.status === 'loaded' && !controls.isEmpty && (
        <ul>
          {state.items.map((course) => (
            <li key={course.id}>
              {course.title} — 정원 {course.capacity} · 확정 {course.enrolledCount} · {course.status}
              {' '}
              <button type="button" onClick={() => onEditCourse(course.id)}>
                수정
              </button>
              {/* REQ-ADM-009 — 마감된 강좌를 다시 마감할 조작은 노출하지 않는다. */}
              {course.status !== 'CLOSED' && (
                <button type="button" disabled={closingId === course.id} onClick={() => handleClose(course.id)}>
                  마감
                </button>
              )}
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
