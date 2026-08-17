import { useEffect, useState } from 'react'
import { apiFetch, ApiError } from './api/client'
import type { CoursePage } from './api/types'

// M1 임시 화면 — 화면 흐름·라우팅은 M2~M6의 범위다(design.md §A.6). 이 컴포넌트의
// 유일한 목적은 GET /api/courses가 Vite 프록시 없이 교차 오리진으로 실제 성공하는지
// 브라우저에서 육안 확인하는 것이다(plan.md M1 완료 판정 / E10, AC-FE-901).
type LoadState =
  | { status: 'loading' }
  | { status: 'success'; data: CoursePage }
  | { status: 'error'; message: string }

function App() {
  const [state, setState] = useState<LoadState>({ status: 'loading' })

  useEffect(() => {
    let cancelled = false

    apiFetch<CoursePage>('/api/courses?page=0&size=10')
      .then((data) => {
        if (!cancelled) {
          setState({ status: 'success', data })
        }
      })
      .catch((error: unknown) => {
        if (cancelled) return
        const message = error instanceof ApiError ? error.normalized.message : '알 수 없는 오류가 발생했습니다.'
        setState({ status: 'error', message })
      })

    return () => {
      cancelled = true
    }
  }, [])

  return (
    <main>
      <h1>OpenClass — 워크스페이스 부트스트랩 (M1)</h1>
      <p>
        이 화면은 CORS 실동작(<code>GET /api/courses</code>, 프록시 미사용)을 육안으로 확인하기 위한 M1
        확인용 화면입니다.
      </p>
      {state.status === 'loading' && <p>강좌 목록을 불러오는 중입니다…</p>}
      {state.status === 'error' && <p role="alert">오류: {state.message}</p>}
      {state.status === 'success' && (
        <>
          <p>
            총 {state.data.totalElements}건 중 {state.data.items.length}건 표시
          </p>
          <ul>
            {state.data.items.map((course) => (
              <li key={course.id}>
                {course.title} ({course.status})
              </li>
            ))}
          </ul>
        </>
      )}
    </main>
  )
}

export default App
