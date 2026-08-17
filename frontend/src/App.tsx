import { useState, type ReactNode } from 'react'
import { BrowserRouter, Navigate, Route, Routes, useNavigate, useParams } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SessionProvider } from './session/SessionContext'
import { useSession } from './session/useSession'
import { LogoutButton } from './session/LogoutButton'
import { SignupPage } from './pages/SignupPage'
import { LoginPage } from './pages/LoginPage'
import { CatalogSection } from './catalog/CatalogSection'
import { RequireAuth } from './routing/guards'
import { evaluateRoleGuard } from './routing/guardLogic'
import { RequestStatusPage } from './enrollment/RequestStatusPage'
import { AdminCoursesPage } from './admin/AdminCoursesPage'
import { AdminCourseFormPage } from './admin/AdminCourseFormPage'
import { resolveAdminGuardFallback, shouldShowAdminMenu } from './admin/adminModel'
import { MyEnrollmentsPage } from './cancellation/MyEnrollmentsPage'
import { MyWaitlistPage } from './cancellation/MyWaitlistPage'

// M2 — 회원가입·로그인·세션 수립/복원/폐기의 최소 실행 흐름(plan.md M2 완료
// 판정: 브라우저에서 회원가입→로그인→새로고침 유지→탭 종료 후 소멸 확인).
// M3 — 강좌 카탈로그(REQ-CAT-001~006)는 세션 여부와 무관하게 항상 열람
// 가능해야 하므로(REQ-CAT-006), 인증/비인증 두 화면 모두에서 렌더링한다.
// M4 — `/requests/:requestId`가 design.md §A.6이 정의한 첫 일급 URL 경로다
// (REQ-ENR-011의 새로고침·직접 진입 요구가 라우터 없이는 성립하지 않는다 —
// 과업 지시 B1). 다른 화면(회원가입·로그인·카탈로그)은 여전히 App.tsx의
// 기존 콜백 기반 지역 상태(useState) 전환을 그대로 유지한다 — 이 화면들은
// URL 직접 진입이 요구사항이 아니므로 전환할 이유가 없다(과업 지시 B1의
// 부분 전환 허용).
// M6 — `/enrollments/mine`·`/waitlist/mine`을 추가한다(REQ-CNL-006). 확정
// 취소는 응답의 requestId로 기존 `/requests/:requestId` 폴링 경로로 이동한다
// (REQ-CNL-001·002 — enrollment/RequestStatusPage.tsx·useRequestStatus.ts를
// 그대로 재사용, 신규 폴링 코드 없음). 대기명단 취소는 200 동기 응답이므로
// 같은 화면에 머물러 재조회한다(REQ-CNL-003·009).
type Screen = 'signup' | 'login'

function AuthenticatedView() {
  const { session } = useSession()
  const navigate = useNavigate()
  if (session.status !== 'authenticated') {
    return null
  }
  return (
    <main>
      <h1>OpenClass</h1>
      <p>
        {session.email}로 로그인되어 있습니다. (역할: {session.role})
      </p>
      <LogoutButton />
      {/* REQ-CNL-006 — 인증된 회원이면 항상 노출한다(역할 무관). */}
      <button type="button" onClick={() => navigate('/enrollments/mine')}>
        내 수강신청
      </button>
      <button type="button" onClick={() => navigate('/waitlist/mine')}>
        내 대기명단
      </button>
      {/* REQ-ADM-001 — 관리자 화면 진입 수단은 role === 'ADMIN'일 때만 노출한다. */}
      {shouldShowAdminMenu(session.role) && (
        <button type="button" onClick={() => navigate('/admin/courses')}>
          관리자
        </button>
      )}
      <CatalogSection />
    </main>
  )
}

/**
 * `/admin/**` 경로 진입점의 공통 가드 — REQ-ADM-002. `RequireRole`(guards.tsx)
 * 은 단일 fallback만 지원해 "세션 없음"과 "권한 부족"을 구별하지 못하므로,
 * 이 화면군만 `evaluateRoleGuard` 판정을 직접 소비해 두 사유를 분기한다
 * (`resolveAdminGuardFallback`, adminModel.ts). 세션이 없으면 로그인 화면으로
 * 유도하고, 세션은 있으나 역할이 부족하면 화면을 렌더링하지 않고 권한 없음만
 * 안내한다(design.md §A.6, AC-FE-082) — REQ-ADM-003이 요구하는 대로 이 판정은
 * 보안 통제가 아니라 UX 장치일 뿐이며, 실제 강제는 백엔드의 403이다
 * (spec.md §A.7, INV-FE-005).
 */
function AdminRoute({ children }: { children: ReactNode }) {
  const { session } = useSession()
  const result = evaluateRoleGuard(session, 'ADMIN')
  const fallback = resolveAdminGuardFallback(result)
  if (fallback === null) {
    return <>{children}</>
  }
  if (fallback === 'redirect-home') {
    return <Navigate to="/" replace />
  }
  return <p role="alert">이 화면에 접근할 권한이 없습니다.</p>
}

function AdminCoursesRoute() {
  const navigate = useNavigate()
  const { session } = useSession()
  const token = session.status === 'authenticated' ? session.token : ''
  return (
    <AdminRoute>
      <AdminCoursesPage
        token={token}
        onCreateCourse={() => navigate('/admin/courses/new')}
        onEditCourse={(courseId) => navigate(`/admin/courses/${courseId}/edit`)}
      />
    </AdminRoute>
  )
}

function AdminCourseCreateRoute() {
  const navigate = useNavigate()
  const { session } = useSession()
  const token = session.status === 'authenticated' ? session.token : ''
  return (
    <AdminRoute>
      <AdminCourseFormPage
        token={token}
        onSaved={() => navigate('/admin/courses')}
        onCancel={() => navigate('/admin/courses')}
      />
    </AdminRoute>
  )
}

function AdminCourseEditRoute() {
  const navigate = useNavigate()
  const { session } = useSession()
  const { id } = useParams<{ id: string }>()
  const token = session.status === 'authenticated' ? session.token : ''
  const parsedId = Number(id)
  return (
    <AdminRoute>
      {Number.isFinite(parsedId) ? (
        <AdminCourseFormPage
          token={token}
          courseId={parsedId}
          onSaved={() => navigate('/admin/courses')}
          onCancel={() => navigate('/admin/courses')}
        />
      ) : (
        <p role="alert">잘못된 강좌 식별자입니다.</p>
      )}
    </AdminRoute>
  )
}

function AnonymousView() {
  const [screen, setScreen] = useState<Screen>('login')

  return (
    <main>
      <h1>OpenClass</h1>
      {screen === 'signup' ? (
        <>
          <SignupPage onSignupSuccess={() => setScreen('login')} />
          <button type="button" onClick={() => setScreen('login')}>
            이미 계정이 있으신가요? 로그인
          </button>
        </>
      ) : (
        <>
          <LoginPage onLoginSuccess={() => setScreen('login')} />
          <button type="button" onClick={() => setScreen('signup')}>
            계정이 없으신가요? 회원가입
          </button>
        </>
      )}
      {/* REQ-CAT-006 — 세션이 없는 방문자도 카탈로그를 열람할 수 있다. */}
      <CatalogSection />
    </main>
  )
}

function Shell() {
  const { session } = useSession()
  return session.status === 'authenticated' ? <AuthenticatedView /> : <AnonymousView />
}

/**
 * `/requests/:requestId` 경로 진입점 — REQ-SES-009(인증 가드) + 식별자
 * 파싱을 담당하고, 실제 폴링/렌더링은 `RequestStatusPage`에 위임한다. 세션이
 * 없으면 `/`로 유도한다(AnonymousView가 기본으로 로그인 화면을 보여준다 —
 * REQ-SES-009 "로그인 화면으로 유도").
 */
function RequestStatusRoute() {
  const { requestId } = useParams<{ requestId: string }>()
  const { session } = useSession()
  const parsedId = Number(requestId)

  return (
    <RequireAuth fallback={<Navigate to="/" replace />}>
      {session.status === 'authenticated' && Number.isFinite(parsedId) ? (
        <RequestStatusPage requestId={parsedId} token={session.token} />
      ) : (
        <p role="alert">잘못된 요청 식별자입니다.</p>
      )}
    </RequireAuth>
  )
}

/**
 * `/enrollments/mine` 진입점 — REQ-SES-009 인증 가드. 세션이 없으면 `/`로
 * 유도한다(AnonymousView가 기본으로 로그인 화면을 보여준다).
 */
function MyEnrollmentsRoute() {
  const { session } = useSession()
  return (
    <RequireAuth fallback={<Navigate to="/" replace />}>
      {session.status === 'authenticated' ? <MyEnrollmentsPage token={session.token} /> : null}
    </RequireAuth>
  )
}

/** `/waitlist/mine` 진입점 — REQ-SES-009 인증 가드. `MyEnrollmentsRoute`와 동일한 구조. */
function MyWaitlistRoute() {
  const { session } = useSession()
  return (
    <RequireAuth fallback={<Navigate to="/" replace />}>
      {session.status === 'authenticated' ? <MyWaitlistPage token={session.token} /> : null}
    </RequireAuth>
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/requests/:requestId" element={<RequestStatusRoute />} />
      <Route path="/enrollments/mine" element={<MyEnrollmentsRoute />} />
      <Route path="/waitlist/mine" element={<MyWaitlistRoute />} />
      <Route path="/admin/courses" element={<AdminCoursesRoute />} />
      <Route path="/admin/courses/new" element={<AdminCourseCreateRoute />} />
      <Route path="/admin/courses/:id/edit" element={<AdminCourseEditRoute />} />
      <Route path="*" element={<Shell />} />
    </Routes>
  )
}

// M4 — TanStack Query는 폴링 스케줄(§C.3) 전용으로만 쓴다(plan.md §C.6 —
// 이 SPEC에서 사다리 4단계를 정당화하는 유일한 항목). 기본 캐시 재시도
// 설정은 useRequestStatus.ts가 쿼리별로 명시적으로 끈다(`retry: false`).
const queryClient = new QueryClient()

function App() {
  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <SessionProvider>
          <AppRoutes />
        </SessionProvider>
      </QueryClientProvider>
    </BrowserRouter>
  )
}

export default App
