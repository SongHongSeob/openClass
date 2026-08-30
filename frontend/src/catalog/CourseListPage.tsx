// @MX:NOTE: [AUTO] 강좌 목록 화면(REQ-CAT-001~003·006). 세션 없이도 열람
// 가능하다 — 이 컴포넌트는 세션을 전혀 참조하지 않는다(REQ-CAT-006). 페이지
// 이동은 백엔드 메타데이터 기반이며(REQ-CAT-002), 자체 분할하지 않는다.

import { useEffect, useState, type FormEvent } from 'react'
import { getCourses } from '../api/endpoints'
import { ApiError } from '../api/client'
import type { Course } from '../api/types'
import { computePageControls } from './catalogModel'
import { Button } from '@/components/ui/button'
import { Alert } from '@/components/ui/alert'
import { Badge } from '@/components/ui/badge'
import { Input } from '@/components/ui/input'

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
  const [keywordInput, setKeywordInput] = useState('')
  // REQ-CAT-007(Amendment 1) — 실제 조회에 쓰이는 검색어는 제출(Enter/버튼)
  // 시점에만 갱신한다. 입력 중인 값(keywordInput)과 분리해 타이핑마다 재조회를
  // 유발하지 않는다.
  const [keyword, setKeyword] = useState('')
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
    getCourses(page, PAGE_SIZE, keyword)
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
  }, [page, keyword])

  const handleSearchSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setPage(0)
    setKeyword(keywordInput.trim())
  }

  return (
    <section className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold text-neutral-900 dark:text-neutral-100">강좌 목록</h2>
      <form onSubmit={handleSearchSubmit} className="flex items-center gap-2" role="search">
        <Input
          type="search"
          value={keywordInput}
          onChange={(event) => setKeywordInput(event.target.value)}
          placeholder="강좌명으로 검색"
          aria-label="강좌명으로 검색"
          className="max-w-xs"
        />
        <Button type="submit" variant="secondary" size="sm">
          검색
        </Button>
      </form>
      {state.status === 'loading' && <p className="text-sm text-neutral-500">불러오는 중…</p>}
      {state.status === 'error' && <Alert role="alert" tone="error">{state.message}</Alert>}
      {state.status === 'loaded' && controls.isEmpty && (
        <p className="text-sm text-neutral-500">표시할 강좌가 없습니다.</p>
      )}
      {state.status === 'loaded' && !controls.isEmpty && (
        <div className="overflow-x-auto rounded-md border border-neutral-200 dark:border-neutral-800">
          <table className="w-full border-collapse text-left text-sm">
            <thead>
              <tr className="border-b border-neutral-200 bg-neutral-50 dark:border-neutral-800 dark:bg-neutral-900">
                <th scope="col" className="px-4 py-2 font-semibold text-neutral-900 dark:text-neutral-100">
                  강좌명
                </th>
                <th scope="col" className="px-4 py-2 font-semibold text-neutral-900 dark:text-neutral-100">
                  정원
                </th>
                <th scope="col" className="px-4 py-2 font-semibold text-neutral-900 dark:text-neutral-100">
                  확정
                </th>
                <th scope="col" className="px-4 py-2 font-semibold text-neutral-900 dark:text-neutral-100">
                  잔여
                </th>
                <th scope="col" className="px-4 py-2 font-semibold text-neutral-900 dark:text-neutral-100">
                  상태
                </th>
              </tr>
            </thead>
            <tbody>
              {state.items.map((course, index) => (
                <tr
                  key={course.id}
                  className={
                    index % 2 === 0
                      ? 'border-b border-neutral-200 last:border-b-0 hover:bg-neutral-50 dark:border-neutral-800 dark:hover:bg-neutral-900'
                      : 'border-b border-neutral-200 bg-neutral-50/50 last:border-b-0 hover:bg-neutral-100 dark:border-neutral-800 dark:bg-neutral-900/40 dark:hover:bg-neutral-800'
                  }
                >
                  <td className="px-4 py-2">
                    <Button
                      type="button"
                      variant="ghost"
                      className="h-auto justify-start px-0 text-left font-semibold"
                      onClick={() => onSelectCourse(course.id)}
                    >
                      {course.title}
                    </Button>
                  </td>
                  <td className="px-4 py-2 text-neutral-600 dark:text-neutral-400">{course.capacity}</td>
                  <td className="px-4 py-2 text-neutral-600 dark:text-neutral-400">{course.enrolledCount}</td>
                  <td className="px-4 py-2 text-neutral-600 dark:text-neutral-400">{course.remainingCapacity}</td>
                  <td className="px-4 py-2">
                    <Badge variant={course.status === 'CLOSED' ? 'neutral' : 'accent'}>{course.status}</Badge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {state.status === 'loaded' && !controls.isEmpty && (
        <p className="flex items-center gap-3 text-sm text-neutral-600 dark:text-neutral-400">
          <Button type="button" variant="outline" size="sm" disabled={!controls.hasPrevious} onClick={() => setPage((current) => current - 1)}>
            이전
          </Button>
          <span>
            {controls.currentPage + 1} / {Math.max(controls.totalPages, 1)}
          </span>
          <Button type="button" variant="outline" size="sm" disabled={!controls.hasNext} onClick={() => setPage((current) => current + 1)}>
            다음
          </Button>
        </p>
      )}
    </section>
  )
}
