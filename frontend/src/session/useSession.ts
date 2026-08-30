// @MX:NOTE: [AUTO] `useSession()` 훅 전용 파일 — react/only-export-components
// 린트 규칙(Fast Refresh 안정성) 때문에 컴포넌트 파일(`SessionContext.tsx`)과
// 분리했다. 세션 상태의 소유권은 `SessionContext.tsx`의 `SessionProvider`에
// 있다.

import { useContext } from 'react'
import { SessionContext, type SessionContextValue } from './sessionContextInstance'

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext)
  if (!context) {
    throw new Error('useSession must be used within a SessionProvider')
  }
  return context
}
