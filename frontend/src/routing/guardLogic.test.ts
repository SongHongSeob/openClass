import { describe, expect, it } from 'vitest'
import { evaluateAuthGuard, evaluateRoleGuard } from './guardLogic'
import { deriveSessionState } from '../session/sessionState'

function makeToken(payload: Record<string, unknown>): string {
  const base64UrlEncode = (input: string): string =>
    btoa(input).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  const header = base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = base64UrlEncode(JSON.stringify(payload))
  return `${header}.${body}.signature-not-verified`
}

const FUTURE_EXP = Math.floor(Date.now() / 1000) + 3600
const ANONYMOUS = deriveSessionState(null)
const MEMBER_SESSION = deriveSessionState(makeToken({ sub: 'member@example.com', role: 'MEMBER', exp: FUTURE_EXP }))
const ADMIN_SESSION = deriveSessionState(makeToken({ sub: 'admin@example.com', role: 'ADMIN', exp: FUTURE_EXP }))

describe('evaluateAuthGuard — REQ-SES-009', () => {
  it('denies with reason no-session when there is no session', () => {
    expect(evaluateAuthGuard(ANONYMOUS)).toEqual({ allowed: false, reason: 'no-session' })
  })

  it('allows when a session is authenticated', () => {
    expect(evaluateAuthGuard(MEMBER_SESSION)).toEqual({ allowed: true })
  })
})

describe('evaluateRoleGuard — REQ-ADM-002', () => {
  it('denies with reason no-session (not insufficient-role) when there is no session at all', () => {
    // design.md §A.6 — 세션 부재와 권한 부족은 서로 다른 안내를 해야 한다.
    expect(evaluateRoleGuard(ANONYMOUS, 'ADMIN')).toEqual({ allowed: false, reason: 'no-session' })
  })

  it('denies with reason insufficient-role when the session role does not match', () => {
    expect(evaluateRoleGuard(MEMBER_SESSION, 'ADMIN')).toEqual({ allowed: false, reason: 'insufficient-role' })
  })

  it('allows when the session role matches the required role', () => {
    expect(evaluateRoleGuard(ADMIN_SESSION, 'ADMIN')).toEqual({ allowed: true })
  })

  it('allows a MEMBER session against a MEMBER-required guard', () => {
    expect(evaluateRoleGuard(MEMBER_SESSION, 'MEMBER')).toEqual({ allowed: true })
  })
})

describe('REQ-SES-008 / INV-FE-005 — role is display-only, never a security boundary', () => {
  it('a forged role client-side structurally passes evaluateRoleGuard, proving the guard is UX-only', () => {
    // jwt.ts never verifies a signature, so any role string decodes and
    // round-trips unchanged (see jwt.test.ts). This test demonstrates the
    // consequence one layer up: evaluateRoleGuard grants "allowed" purely
    // based on that unverified value. This is NOT a vulnerability by itself
    // — the API call this would gate is still rejected by the backend's real
    // signature + role check (§A.7) — but it IS the proof that no code path
    // may treat this guard's "allowed" result as an authorization decision.
    const forgedAdminToken = makeToken({ sub: 'attacker@example.com', role: 'ADMIN', exp: FUTURE_EXP })
    const forgedSession = deriveSessionState(forgedAdminToken)
    expect(evaluateRoleGuard(forgedSession, 'ADMIN')).toEqual({ allowed: true })
  })
})
