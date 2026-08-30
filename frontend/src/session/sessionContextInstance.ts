// @MX:NOTE: [AUTO] React Context 인스턴스 자체만 담는 파일. `react/
// only-export-components` 린트 규칙(Fast Refresh 안정성)을 만족시키기 위해
// `SessionContext.tsx`(컴포넌트 전용)·`useSession.ts`(훅 전용)와 분리했다.
// 세션 상태의 소유권은 여전히 `SessionContext.tsx`의 `SessionProvider`에
// 있다 — 이 파일은 두 소비자가 공유하는 컨텍스트 객체만 정의한다.

import { createContext } from 'react'
import type { SessionState } from './sessionState'

export interface SessionContextValue {
  session: SessionState
  /** 로그인 성공 시 호출한다 — 토큰을 저장하고 세션을 수립한다(REQ-SES-002). */
  establishSession: (token: string) => void
  /** 로그아웃 또는 401 감지 시 호출한다 — 토큰을 제거하고 세션을 폐기한다
   * (REQ-SES-005, REQ-SES-007). */
  discardSession: () => void
}

export const SessionContext = createContext<SessionContextValue | null>(null)
