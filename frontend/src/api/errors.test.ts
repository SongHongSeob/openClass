import { describe, expect, it } from 'vitest'
import { normalizeFetchFailure, normalizeResponseError } from './errors'

describe('normalizeFetchFailure — step 1 (no response received)', () => {
  it('classifies as network, never as a server error (REQ-ERR-005, AC-FE-007)', () => {
    const result = normalizeFetchFailure()
    expect(result.classification).toBe('network')
    // plan.md §C.2 step 1: this classification must not be phrased as "서버 오류"
    // (server-side fault) — it is "server unreachable", a distinct diagnosis.
    expect(result.message).not.toMatch(/서버\s*오류/)
  })
})

describe('normalizeResponseError — judgment order is the contract (plan.md §C.2)', () => {
  it('AC-FE-006: classifies 401 as session-expired when body HAS a code field', () => {
    const result = normalizeResponseError(401, { code: 'INVALID_CREDENTIALS', message: 'bad creds' })
    expect(result.classification).toBe('session-expired')
  })

  it('AC-FE-006: classifies 401 as session-expired when body has NO code field (F2)', () => {
    const result = normalizeResponseError(401, { timestamp: '...', status: 401, error: 'Unauthorized', path: '/api/x' })
    expect(result.classification).toBe('session-expired')
  })

  it('401-before-code-field: a code field present on a 401 body does NOT route to domain classification', () => {
    // Regression guard for plan.md §C.2's core decision: step 2 (401) MUST be
    // checked before step 3 (code field), even though this body has a `code`
    // that (if evaluated in isolation) would look like a domain error.
    const result = normalizeResponseError(401, { code: 'DUPLICATE_EMAIL', message: 'irrelevant' })
    expect(result.classification).toBe('session-expired')
  })

  it('AC-FE-005a: classifies a non-401 body WITH a code field (F1) as domain, message selected by code', () => {
    const result = normalizeResponseError(404, { code: 'COURSE_NOT_FOUND', message: 'raw backend text' })
    expect(result.classification).toBe('domain')
    expect(result.code).toBe('COURSE_NOT_FOUND')
    expect(result.message).not.toBe('raw backend text')
  })

  it('AC-FE-005b: classifies 403 WITHOUT a code field (F2) as status-code, no raw body exposed', () => {
    const result = normalizeResponseError(403, { timestamp: '...', status: 403, error: 'Forbidden', path: '/api/admin/courses' })
    expect(result.classification).toBe('status-code')
    expect(result.message).not.toMatch(/timestamp|Forbidden|path/)
  })

  it('AC-FE-005b: classifies 400 WITHOUT a code field (F3 — @Valid failure) as status-code', () => {
    const result = normalizeResponseError(400, { errors: [{ field: 'password', defaultMessage: 'size must be between 8 and 2147483647' }] })
    expect(result.classification).toBe('status-code')
    expect(result.message).not.toMatch(/password|defaultMessage/)
  })

  it('AC-FE-010 / INV-FE-006: an unknown body shape never throws and still resolves to status-code', () => {
    expect(() => normalizeResponseError(500, undefined)).not.toThrow()
    expect(() => normalizeResponseError(500, null)).not.toThrow()
    expect(() => normalizeResponseError(500, 'plain text body')).not.toThrow()
    expect(() => normalizeResponseError(500, [1, 2, 3])).not.toThrow()

    const result = normalizeResponseError(500, 'plain text body')
    expect(result.classification).toBe('status-code')
  })

  it('AC-FE-009 / REQ-ERR-006: message never contains a stack trace, internal path, or exception class name', () => {
    const result = normalizeResponseError(500, {
      timestamp: '2026-08-17T09:00:00.000+00:00',
      status: 500,
      error: 'Internal Server Error',
      path: '/api/courses',
      trace: 'java.lang.NullPointerException: at com.hongseob...',
    })
    expect(result.message).not.toMatch(/java\.lang|NullPointerException|com\.hongseob|\/api\/courses/)
  })

  it('a code field whose value is not a string is treated as absent (no domain misclassification)', () => {
    const result = normalizeResponseError(404, { code: 12345, message: 'x' })
    expect(result.classification).toBe('status-code')
  })
})
