// @MX:NOTE: [AUTO] 인증·역할 가드의 순수 판정 로직 — 라우터 구현과 독립적이다.
// M2는 이 판정 함수와 그 소비 컴포넌트(`guards.tsx`)만 제공한다. 실제로
// 가드가 감쌀 화면(요청 상태·내 수강신청·관리자 등)은 M4~M6이 만든다
// (design.md §A.6, 위임 지시).
//
// **이 가드는 보안 통제가 아니라 UX 편의 장치다** (REQ-ADM-003, spec.md
// §A.7). 클라이언트 코드는 사용자가 임의로 변경할 수 있으므로, 이 판정을
// 우회한 요청이 발생해도 실제로 막는 것은 백엔드의 401/403이다
// (INV-FE-005). 이 판정은 "화면을 보여줄지"만 결정하며 "요청을 허용할지"는
// 결정하지 않는다 — API 호출 여부는 이 모듈과 무관하게 이루어진다.

import type { MemberRole } from '../api/types'
import type { SessionState } from '../session/sessionState'

export type GuardResult =
  | { allowed: true }
  | { allowed: false; reason: 'no-session' }
  | { allowed: false; reason: 'insufficient-role' }

/** REQ-SES-009 — 세션이 없으면 인증 필요 화면 진입을 허용하지 않는다. */
export function evaluateAuthGuard(session: SessionState): GuardResult {
  if (session.status !== 'authenticated') {
    return { allowed: false, reason: 'no-session' }
  }
  return { allowed: true }
}

/**
 * REQ-ADM-002 — 세션은 있으나 요구 역할이 아니면 권한 없음. 세션 자체가
 * 없는 경우는 `evaluateAuthGuard`와 동일하게 `no-session`으로 판정한다
 * (design.md §A.6 — 인증 가드와 역할 가드는 서로 다른 안내를 하기 위해
 * 분리된 계층이다).
 *
 * `session.role`은 `jwt.ts`가 서명 검증 없이 디코드한 값이다(REQ-SES-008) —
 * 이 함수의 판정은 표시·UX 목적에 한정되며, 실제 권한 강제는 백엔드가
 * 담당한다.
 */
export function evaluateRoleGuard(session: SessionState, requiredRole: MemberRole): GuardResult {
  const authResult = evaluateAuthGuard(session)
  if (!authResult.allowed) {
    return authResult
  }
  if (session.status === 'authenticated' && session.role !== requiredRole) {
    return { allowed: false, reason: 'insufficient-role' }
  }
  return { allowed: true }
}
