import { describe, expect, it } from 'vitest'
import { deriveSessionState, sessionReducer, INITIAL_SESSION_STATE } from './sessionState'

function makeToken(payload: Record<string, unknown>): string {
  const base64UrlEncode = (input: string): string =>
    btoa(input).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  const header = base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = base64UrlEncode(JSON.stringify(payload))
  return `${header}.${body}.signature-not-verified`
}

const FUTURE_EXP = Math.floor(Date.now() / 1000) + 3600
const PAST_EXP = Math.floor(Date.now() / 1000) - 3600

describe('deriveSessionState — design.md §A.5 수립/복원 공통 규칙', () => {
  it('returns anonymous for a null token (no session)', () => {
    expect(deriveSessionState(null)).toEqual({ status: 'anonymous' })
  })

  it('returns authenticated for a valid, unexpired token', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'MEMBER', exp: FUTURE_EXP })
    const result = deriveSessionState(token)
    expect(result).toEqual({
      status: 'authenticated',
      token,
      email: 'user@example.com',
      role: 'MEMBER',
      exp: FUTURE_EXP,
    })
  })

  it('AC-FE-033: returns anonymous (no throw) for a corrupted token', () => {
    expect(() => deriveSessionState('not-a-jwt')).not.toThrow()
    expect(deriveSessionState('not-a-jwt')).toEqual({ status: 'anonymous' })
  })

  it('AC-FE-032: returns anonymous — no server round trip — when exp has already passed', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'MEMBER', exp: PAST_EXP })
    expect(deriveSessionState(token)).toEqual({ status: 'anonymous' })
  })

  it('normalizes an unrecognized role claim to MEMBER (defensive — backend enum is closed today)', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'SOMETHING_ELSE', exp: FUTURE_EXP })
    const result = deriveSessionState(token)
    expect(result.status).toBe('authenticated')
    if (result.status === 'authenticated') {
      expect(result.role).toBe('MEMBER')
    }
  })

  it('preserves ADMIN role', () => {
    const token = makeToken({ sub: 'admin@example.com', role: 'ADMIN', exp: FUTURE_EXP })
    const result = deriveSessionState(token)
    expect(result.status).toBe('authenticated')
    if (result.status === 'authenticated') {
      expect(result.role).toBe('ADMIN')
    }
  })
})

describe('sessionReducer — ESTABLISH / RESTORE / DISCARD transitions', () => {
  it('starts from INITIAL_SESSION_STATE as anonymous', () => {
    expect(INITIAL_SESSION_STATE).toEqual({ status: 'anonymous' })
  })

  it('ESTABLISH transitions from anonymous to authenticated', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'MEMBER', exp: FUTURE_EXP })
    const next = sessionReducer(INITIAL_SESSION_STATE, { type: 'ESTABLISH', token })
    expect(next.status).toBe('authenticated')
  })

  it('RESTORE with a null token yields anonymous', () => {
    const next = sessionReducer(INITIAL_SESSION_STATE, { type: 'RESTORE', token: null })
    expect(next).toEqual({ status: 'anonymous' })
  })

  it('RESTORE with an expired token yields anonymous (AC-FE-032 via the reducer path)', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'MEMBER', exp: PAST_EXP })
    const next = sessionReducer(INITIAL_SESSION_STATE, { type: 'RESTORE', token })
    expect(next).toEqual({ status: 'anonymous' })
  })

  it('DISCARD transitions an authenticated state back to anonymous (REQ-SES-005/007)', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'MEMBER', exp: FUTURE_EXP })
    const authenticated = sessionReducer(INITIAL_SESSION_STATE, { type: 'ESTABLISH', token })
    const discarded = sessionReducer(authenticated, { type: 'DISCARD' })
    expect(discarded).toEqual({ status: 'anonymous' })
  })
})
