import { useState } from 'react'
import { SessionProvider } from './session/SessionContext'
import { useSession } from './session/useSession'
import { LogoutButton } from './session/LogoutButton'
import { SignupPage } from './pages/SignupPage'
import { LoginPage } from './pages/LoginPage'
import { CatalogSection } from './catalog/CatalogSection'

// M2 — 회원가입·로그인·세션 수립/복원/폐기의 최소 실행 흐름(plan.md M2 완료
// 판정: 브라우저에서 회원가입→로그인→새로고침 유지→탭 종료 후 소멸 확인).
// M3 — 강좌 카탈로그(REQ-CAT-001~006)는 세션 여부와 무관하게 항상 열람
// 가능해야 하므로(REQ-CAT-006), 인증/비인증 두 화면 모두에서 렌더링한다.
// 라우터는 아직 배선하지 않는다 — 화면 간 이동은 이 컴포넌트와
// catalog/CatalogSection.tsx의 지역 상태(useState)로 처리한다. 실제 다중
// 경로 라우팅(React Router, plan.md §C.6)은 수강신청 등 URL 직접 진입이
// 필요해지는 이후 마일스톤이 도입한다. 인증 가드(routing/guards.tsx)는 이
// 화면에서 아직 사용하지 않는다 — M2·M3의 화면은 전부 공개 화면이기 때문이다.
type Screen = 'signup' | 'login'

function AuthenticatedView() {
  const { session } = useSession()
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
      <CatalogSection />
    </main>
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

function App() {
  return (
    <SessionProvider>
      <Shell />
    </SessionProvider>
  )
}

export default App
