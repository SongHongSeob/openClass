// @MX:ANCHOR: [AUTO] 세션 상태 전이의 순수 로직 — 수립·복원·폐기 3가지 전이를
// 모두 이 모듈이 정의한다(design.md §A.5). SessionContext.tsx는 이 모듈을
// React useReducer에 연결만 할 뿐, 전이 규칙 자체를 재구현하지 않는다.
// @MX:REASON: plan.md §C.6 — React Context + useReducer로 결정된 유일한 전역
// 클라이언트 상태(세션)다. 전이 판정이 여러 지점에 흩어지면 만료·손상 토큰
// 처리(AC-FE-032/033)가 한쪽 경로에서만 동작할 위험이 있다.

import { decodeJwtPayload, isExpired } from './jwt'
import type { MemberRole } from '../api/types'

export type SessionState =
  | { status: 'anonymous' }
  | { status: 'authenticated'; token: string; email: string; role: MemberRole; exp: number }

export type SessionAction =
  | { type: 'ESTABLISH'; token: string }
  | { type: 'RESTORE'; token: string | null }
  | { type: 'DISCARD' }

/**
 * 토큰(또는 부재)으로부터 세션 상태를 유도한다. 로그인 직후(`ESTABLISH`)와
 * 앱 최초 렌더 시 복원(`RESTORE`)이 동일한 규칙을 공유한다 — design.md §A.5가
 * 두 경로를 같은 판정으로 정의한다.
 *
 * `nowMs`를 주입 가능하게 하여 만료 판정을 테스트에서 고정할 수 있다.
 *
 * - 토큰이 없으면 익명(`anonymous`).
 * - 디코드 실패(손상·형식 위반)면 익명 — 예외를 던지지 않는다(INV-FE-006,
 *   AC-FE-033).
 * - `exp`가 이미 지났으면 익명 — 서버 왕복 없이 즉시 폐기한다(AC-FE-032).
 * - 그 외에는 인증됨.
 */
export function deriveSessionState(token: string | null, nowMs: number = Date.now()): SessionState {
  if (!token) {
    return { status: 'anonymous' }
  }

  const payload = decodeJwtPayload(token)
  if (!payload) {
    return { status: 'anonymous' }
  }

  if (isExpired(payload.exp, nowMs)) {
    return { status: 'anonymous' }
  }

  return {
    status: 'authenticated',
    token,
    email: payload.sub,
    role: normalizeRole(payload.role),
    exp: payload.exp,
  }
}

/** `sessionReducer`의 초기 상태 — 토큰이 아직 복원되지 않은 시점. */
export const INITIAL_SESSION_STATE: SessionState = { status: 'anonymous' }

/**
 * `useReducer`에 연결하는 순수 리듀서. 실제 storage I/O는 이 모듈의 책임이
 * 아니다 — `SessionContext.tsx`가 `tokenStorage.ts`와 조합하여 호출한다.
 */
export function sessionReducer(_state: SessionState, action: SessionAction): SessionState {
  switch (action.type) {
    case 'ESTABLISH':
      return deriveSessionState(action.token)
    case 'RESTORE':
      return deriveSessionState(action.token)
    case 'DISCARD':
      return { status: 'anonymous' }
    default:
      return _state
  }
}

function normalizeRole(role: string): MemberRole {
  return role === 'ADMIN' ? 'ADMIN' : 'MEMBER'
}
