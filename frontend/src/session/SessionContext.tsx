// @MX:ANCHOR: [AUTO] 세션 상태의 단일 소스 — 수립·복원·폐기 전이를 이
// Provider가 소유한다(design.md §A.5). 화면은 `useSession()` 훅(`./
// useSession.ts`)으로만 세션을 읽고, sessionStorage나 JWT 디코드를 직접
// 건드리지 않는다.
// @MX:REASON: plan.md §C.6 — React Context + `useReducer`로 결정된 유일한
// 전역 클라이언트 상태다. 다른 지점에서 세션을 갱신하면 sessionStorage와
// 메모리 상태가 어긋날 수 있다.

import { useCallback, useEffect, useMemo, useReducer, type ReactNode } from 'react'
import { clearToken, loadToken, saveToken } from './tokenStorage'
import { sessionReducer, deriveSessionState, type SessionState } from './sessionState'
import { subscribeToSessionExpired } from '../api/client'
import { SessionContext, type SessionContextValue } from './sessionContextInstance'

function readInitialSessionState(): SessionState {
  // design.md §A.5 [복원] — 앱 최초 렌더 시 sessionStorage를 조회한다. 만료·
  // 손상 토큰은 deriveSessionState가 예외 없이 익명으로 정리한다
  // (AC-FE-032/033).
  return deriveSessionState(loadToken(window.sessionStorage))
}

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, dispatch] = useReducer(sessionReducer, undefined, readInitialSessionState)

  const establishSession = useCallback((token: string) => {
    saveToken(window.sessionStorage, token)
    dispatch({ type: 'ESTABLISH', token })
  }, [])

  const discardSession = useCallback(() => {
    clearToken(window.sessionStorage)
    dispatch({ type: 'DISCARD' })
  }, [])

  // REQ-SES-007 — 임의 API 호출이 401(session-expired)로 분류되면 전역적으로
  // 세션을 폐기한다. 이 구독이 유일한 소비 지점이다 — 화면마다 401을 개별
  // 감지하지 않는다.
  useEffect(() => {
    return subscribeToSessionExpired(() => {
      discardSession()
    })
  }, [discardSession])

  const value = useMemo<SessionContextValue>(
    () => ({ session, establishSession, discardSession }),
    [session, establishSession, discardSession],
  )

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>
}
