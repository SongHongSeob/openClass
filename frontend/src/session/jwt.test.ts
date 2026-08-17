import { describe, expect, it } from 'vitest'
import { decodeJwtPayload, isExpired, type JwtPayload } from './jwt'

/** 테스트 전용 인코더 — 백엔드 서명 없이 페이로드 형태만 재현한다.
 * `jwt.ts`의 디코더가 서명을 검증하지 않는다는 사실 자체가 이 헬퍼로 임의의
 * 페이로드를 "유효한 토큰"처럼 만들 수 있다는 것으로 증명된다. */
function makeToken(payload: Record<string, unknown>): string {
  const base64UrlEncode = (input: string): string =>
    btoa(input).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
  const header = base64UrlEncode(JSON.stringify({ alg: 'HS256', typ: 'JWT' }))
  const body = base64UrlEncode(JSON.stringify(payload))
  return `${header}.${body}.signature-not-verified`
}

describe('decodeJwtPayload — AC-FE-035 서명 검증 없음, 표시 목적 한정', () => {
  it('decodes a well-formed payload (sub/role/exp present)', () => {
    const payload = { sub: 'user@example.com', role: 'MEMBER', exp: 9999999999, iat: 1000 }
    const result = decodeJwtPayload(makeToken(payload))
    expect(result).toEqual(payload satisfies JwtPayload)
  })

  it('returns null (not throw) for a token with the wrong segment count', () => {
    expect(decodeJwtPayload('only.two')).toBeNull()
    expect(decodeJwtPayload('')).toBeNull()
    expect(decodeJwtPayload('a.b.c.d')).toBeNull()
  })

  it('AC-FE-033: returns null (not throw) for a non-JSON / corrupted payload segment', () => {
    const corrupted = 'header-part.not-valid-base64url-json!!!.sig'
    expect(() => decodeJwtPayload(corrupted)).not.toThrow()
    expect(decodeJwtPayload(corrupted)).toBeNull()
  })

  it('returns null when a required field is missing (no exp)', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'MEMBER' })
    expect(decodeJwtPayload(token)).toBeNull()
  })

  it('returns null when a required field has the wrong type (exp as string)', () => {
    const token = makeToken({ sub: 'user@example.com', role: 'MEMBER', exp: '9999999999' })
    expect(decodeJwtPayload(token)).toBeNull()
  })

  it('does NOT verify a signature — an arbitrary role value decodes unchanged (proves display-only, REQ-SES-008)', () => {
    const forged = makeToken({ sub: 'attacker@example.com', role: 'ADMIN', exp: 9999999999 })
    const result = decodeJwtPayload(forged)
    // No crypto/signature check occurs here — any role string round-trips.
    // This is the structural proof that this module's output must never be
    // trusted as an authorization decision (spec.md §A.7).
    expect(result?.role).toBe('ADMIN')
  })
})

describe('isExpired — AC-FE-032 선제 만료 판정', () => {
  it('returns false when exp is in the future relative to nowMs', () => {
    expect(isExpired(2000, 1000_000)).toBe(false)
  })

  it('returns true when exp is in the past relative to nowMs', () => {
    expect(isExpired(1000, 2_000_000)).toBe(true)
  })

  it('treats exact equality as expired (exp*1000 <= nowMs)', () => {
    expect(isExpired(1000, 1_000_000)).toBe(true)
  })

  it('defaults nowMs to Date.now() when not provided', () => {
    const farFuture = Math.floor(Date.now() / 1000) + 3600
    expect(isExpired(farFuture)).toBe(false)
    const longPast = Math.floor(Date.now() / 1000) - 3600
    expect(isExpired(longPast)).toBe(true)
  })
})
