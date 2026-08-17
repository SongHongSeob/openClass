// @MX:NOTE: [AUTO] 14개 엔드포인트 호출 함수의 성장 지점(design.md §A.8).
// M2는 인증 2종(signup·login)만 추가한다 — 나머지 12개는 M3~M6이 채운다.

import { apiFetch } from './client'
import type { LoginResult, SignupResult } from './types'

export interface SignupPayload {
  email: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

/** `POST /api/auth/signup` — REQ-SES-001, spec.md §A.4 1번. */
export function signup(payload: SignupPayload): Promise<SignupResult> {
  return apiFetch<SignupResult>('/api/auth/signup', { method: 'POST', body: payload })
}

/** `POST /api/auth/login` — REQ-SES-002, spec.md §A.4 2번. */
export function login(payload: LoginPayload): Promise<LoginResult> {
  return apiFetch<LoginResult>('/api/auth/login', { method: 'POST', body: payload })
}
