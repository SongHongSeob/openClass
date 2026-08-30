// @MX:NOTE: [AUTO] 인증·역할 가드의 컴포넌트 껍데기 — 판정 로직 자체는
// `guardLogic.ts`가 소유한다(재사용·테스트 단순화를 위한 분리). M4~M6이 실제
// 보호 화면을 이 컴포넌트로 감싼다(design.md §A.6).
//
// **보안 통제가 아니다** — REQ-ADM-003, spec.md §A.7. 실제 강제는 백엔드의
// 401/403이다.

import type { ReactNode } from 'react'
import { useSession } from '../session/useSession'
import { evaluateAuthGuard, evaluateRoleGuard } from './guardLogic'
import type { MemberRole } from '../api/types'

export interface RequireAuthProps {
  children: ReactNode
  /** 세션이 없을 때 표시할 대체 화면(예: 로그인 유도 안내) — REQ-SES-009. */
  fallback: ReactNode
}

/** REQ-SES-009 인증 가드. 세션이 없으면 `fallback`을 렌더링한다. */
export function RequireAuth({ children, fallback }: RequireAuthProps) {
  const { session } = useSession()
  const result = evaluateAuthGuard(session)
  return result.allowed ? <>{children}</> : <>{fallback}</>
}

export interface RequireRoleProps extends RequireAuthProps {
  role: MemberRole
}

/** REQ-ADM-002 역할 가드. 세션이 있으나 역할이 다르면 `fallback`을 렌더링한다. */
export function RequireRole({ children, fallback, role }: RequireRoleProps) {
  const { session } = useSession()
  const result = evaluateRoleGuard(session, role)
  return result.allowed ? <>{children}</> : <>{fallback}</>
}
