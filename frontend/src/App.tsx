import type { ReactNode } from 'react'
import { BrowserRouter, Navigate, Outlet, Route, Routes, useNavigate, useParams } from 'react-router'
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
import { Button } from '@/components/ui/button'
import { Alert } from '@/components/ui/alert'
import { Separator } from '@/components/ui/separator'

// M2 — 회원가입·로그인·세션 수립/복원/폐기의 최소 실행 흐름(plan.md M2 완료
// 판정: 브라우저에서 회원가입→로그인→새로고침 유지→탭 종료 후 소멸 확인).
// M3 — 강좌 카탈로그(REQ-CAT-001~006)는 세션 여부와 무관하게 항상 열람
// 가능해야 하므로(REQ-CAT-006), 인증/비인증 두 화면 모두에서 렌더링한다.
// M4 — `/requests/:requestId`가 design.md §A.6이 정의한 첫 일급 URL 경로다
// (REQ-ENR-011의 새로고침·직접 진입 요구가 라우터 없이는 성립하지 않는다 —
// 과업 지시 B1).
// M6 — `/enrollments/mine`·`/waitlist/mine`을 추가한다(REQ-CNL-006). 확정
// 취소는 응답의 requestId로 기존 `/requests/:requestId` 폴링 경로로 이동한다
// (REQ-CNL-001·002 — enrollment/RequestStatusPage.tsx·useRequestStatus.ts를
// 그대로 재사용, 신규 폴링 코드 없음). 대기명단 취소는 200 동기 응답이므로
// 같은 화면에 머물러 재조회한다(REQ-CNL-003·009).
// 사용자 요청 후속 작업 — `/login`·`/signup`을 실 라우트로 분리하고(기존
// App.tsx 지역 state 토글 대체), 좌측 고정 사이드바 + 우측 컨텐츠 레이아웃으로
// 재구성했다. 기존 라우트/가드/리다이렉트 로직(RequireAuth, RequireRole,
// `/` 익명·인증 분기)은 전혀 변경하지 않았다 — 렌더링 위치(사이드바 vs 본문)만
// 바뀌었다.

function AuthenticatedView() {
  const { session } = useSession()
  if (session.status !== 'authenticated') {
    return null
  }
  return (
    <main className="flex flex-col gap-6">
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
  return <Alert role="alert" tone="error" className="mt-6">이 화면에 접근할 권한이 없습니다.</Alert>
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
        <Alert role="alert" tone="error" className="mt-6">잘못된 강좌 식별자입니다.</Alert>
      )}
    </AdminRoute>
  )
}

function AnonymousView() {
  return (
    <main className="flex flex-col gap-6">
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
 * `/login` 경로 진입점 — 기존 `AnonymousView`의 지역 state(`Screen`) 토글을
 * 대체한다. 이미 인증된 세션이면 홈으로 되돌린다. `LoginPage`/`SignupPage`
 * 자체의 폼 필드·제출 핸들러·오류 문구는 전혀 변경하지 않았다 — 성공 시
 * 콜백만 `navigate()`로 바뀌었다.
 */
function LoginRoute() {
  const navigate = useNavigate()
  const { session } = useSession()
  if (session.status === 'authenticated') {
    return <Navigate to="/" replace />
  }
  return (
    <div className="flex flex-col items-center gap-3">
      <LoginPage onLoginSuccess={() => navigate('/')} />
      <Button type="button" variant="ghost" size="sm" onClick={() => navigate('/signup')}>
        계정이 없으신가요? 회원가입
      </Button>
    </div>
  )
}

/**
 * `/signup` 경로 진입점 — 가입 성공 시 기존과 동일하게 로그인 화면으로
 * 유도하되, 지역 state 대신 `navigate('/login')`을 사용한다.
 */
function SignupRoute() {
  const navigate = useNavigate()
  const { session } = useSession()
  if (session.status === 'authenticated') {
    return <Navigate to="/" replace />
  }
  return (
    <div className="flex flex-col items-center gap-3">
      <SignupPage onSignupSuccess={() => navigate('/login')} />
      <Button type="button" variant="ghost" size="sm" onClick={() => navigate('/login')}>
        이미 계정이 있으신가요? 로그인
      </Button>
    </div>
  )
}

/**
 * 좌측 고정 사이드바 — 앱 이름, 내비게이션 링크(강좌목록·내 수강신청·내
 * 대기명단·관리자), 하단 로그인/회원가입(익명) 또는 로그아웃+이메일(인증)을
 * 담당한다. 라우팅·가드 로직은 전혀 갖지 않는다 — 순수 내비게이션 UI다.
 */
function Sidebar() {
  const { session } = useSession()
  const navigate = useNavigate()

  return (
    <aside className="flex w-56 shrink-0 flex-col gap-4 border-r border-neutral-200 bg-white p-4 dark:border-neutral-800 dark:bg-neutral-950">
      <h1 className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">OpenClass</h1>
      <Separator className="my-0" />
      <nav className="flex flex-col gap-1">
        <Button type="button" variant="ghost" className="justify-start" onClick={() => navigate('/')}>
          강좌목록
        </Button>
        {session.status === 'authenticated' && (
          <>
            {/* REQ-CNL-006 — 인증된 회원이면 항상 노출한다(역할 무관). */}
            <Button type="button" variant="ghost" className="justify-start" onClick={() => navigate('/enrollments/mine')}>
              내 수강신청
            </Button>
            <Button type="button" variant="ghost" className="justify-start" onClick={() => navigate('/waitlist/mine')}>
              내 대기명단
            </Button>
            {/* REQ-ADM-001 — 관리자 화면 진입 수단은 role === 'ADMIN'일 때만 노출한다. */}
            {shouldShowAdminMenu(session.role) && (
              <Button type="button" variant="ghost" className="justify-start" onClick={() => navigate('/admin/courses')}>
                관리자
              </Button>
            )}
          </>
        )}
      </nav>
      <div className="mt-auto flex flex-col gap-2">
        <Separator className="my-0" />
        {session.status === 'authenticated' ? (
          <>
            <p className="text-xs text-neutral-600 dark:text-neutral-400">
              {session.email}로 로그인되어 있습니다. (역할: {session.role})
            </p>
            <LogoutButton />
          </>
        ) : (
          <div className="flex flex-col gap-2">
            <Button type="button" variant="secondary" size="sm" onClick={() => navigate('/login')}>
              로그인
            </Button>
            <Button type="button" variant="ghost" size="sm" onClick={() => navigate('/signup')}>
              회원가입
            </Button>
          </div>
        )}
      </div>
    </aside>
  )
}

/**
 * 사이드바 + 본문 영역 레이아웃. `<Outlet />`으로 실제 라우트 컨텐츠를
 * 렌더링한다 — 각 라우트의 가드(RequireAuth/RequireRole/AdminRoute)는
 * `<Outlet />` 안에서 그대로 평가되므로 가드 로직 자체는 변경되지 않는다.
 */
function Layout() {
  return (
    <div className="flex min-h-screen">
      <Sidebar />
      <div className="flex-1 overflow-y-auto p-6">
        <Outlet />
      </div>
    </div>
  )
}

/**
 * `/login`·`/signup` 전용 사이드바 없는 풀스크린 레이아웃. `fixed inset-0`으로
 * `index.css`의 전역 `#root { max-width: 720px; margin: 0 auto }` 제약을
 * 벗어나 뷰포트 전체를 채운다 — 다른 라우트는 여전히 그 제약을 받는다.
 */
function AuthLayout() {
  return (
    <div className="fixed inset-0 flex flex-col items-center justify-center gap-8 bg-neutral-50 p-6 dark:bg-neutral-950">
      <div className="flex flex-col items-center gap-1">
        <h1 className="text-2xl font-bold text-neutral-900 dark:text-neutral-100">OpenClass</h1>
        <p className="text-sm text-neutral-500 dark:text-neutral-400">강좌 신청부터 대기명단까지, 한 곳에서</p>
      </div>
      <Outlet />
    </div>
  )
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
        <Alert role="alert" tone="error" className="mt-6">잘못된 요청 식별자입니다.</Alert>
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
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<LoginRoute />} />
        <Route path="/signup" element={<SignupRoute />} />
      </Route>
      <Route element={<Layout />}>
        <Route path="/requests/:requestId" element={<RequestStatusRoute />} />
        <Route path="/enrollments/mine" element={<MyEnrollmentsRoute />} />
        <Route path="/waitlist/mine" element={<MyWaitlistRoute />} />
        <Route path="/admin/courses" element={<AdminCoursesRoute />} />
        <Route path="/admin/courses/new" element={<AdminCourseCreateRoute />} />
        <Route path="/admin/courses/:id/edit" element={<AdminCourseEditRoute />} />
        <Route path="*" element={<Shell />} />
      </Route>
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
