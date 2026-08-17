import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, ApiError, subscribeToSessionExpired } from './client'

function jsonResponse(status: number, body: unknown): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('apiFetch — header discipline (spec.md §A.5, D6)', () => {
  const originalFetch = globalThis.fetch

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('attaches Authorization only when a token is passed', async () => {
    const fetchSpy = vi.fn().mockResolvedValue(jsonResponse(200, { ok: true }))
    globalThis.fetch = fetchSpy as unknown as typeof fetch

    await apiFetch('/api/courses')

    const [, init] = fetchSpy.mock.calls[0] as [string, RequestInit]
    const headers = init.headers as Record<string, string>
    expect(headers.Authorization).toBeUndefined()
  })

  it('attaches Authorization: Bearer <token> when a token is passed', async () => {
    const fetchSpy = vi.fn().mockResolvedValue(jsonResponse(200, { ok: true }))
    globalThis.fetch = fetchSpy as unknown as typeof fetch

    await apiFetch('/api/enrollments/mine', { token: 'abc.def.ghi' })

    const [, init] = fetchSpy.mock.calls[0] as [string, RequestInit]
    const headers = init.headers as Record<string, string>
    expect(headers.Authorization).toBe('Bearer abc.def.ghi')
  })

  it('never sends credentials: include (backend allowCredentials=false)', async () => {
    const fetchSpy = vi.fn().mockResolvedValue(jsonResponse(200, {}))
    globalThis.fetch = fetchSpy as unknown as typeof fetch

    await apiFetch('/api/courses')

    const [, init] = fetchSpy.mock.calls[0] as [string, RequestInit]
    expect(init.credentials).not.toBe('include')
  })

  it('adds no header beyond Authorization/Content-Type (the 2 the backend allows)', async () => {
    const fetchSpy = vi.fn().mockResolvedValue(jsonResponse(200, {}))
    globalThis.fetch = fetchSpy as unknown as typeof fetch

    await apiFetch('/api/admin/courses', { method: 'POST', body: { title: 'x' }, token: 't' })

    const [, init] = fetchSpy.mock.calls[0] as [string, RequestInit]
    const headers = init.headers as Record<string, string>
    expect(Object.keys(headers).sort()).toEqual(['Authorization', 'Content-Type'])
  })
})

describe('apiFetch — error surfacing routes through the single normalization point', () => {
  const originalFetch = globalThis.fetch

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('a fetch rejection (network/CORS block) throws ApiError classified as network', async () => {
    globalThis.fetch = vi.fn().mockRejectedValue(new TypeError('Failed to fetch')) as unknown as typeof fetch

    await expect(apiFetch('/api/courses')).rejects.toBeInstanceOf(ApiError)
    try {
      await apiFetch('/api/courses')
    } catch (error) {
      expect((error as ApiError).normalized.classification).toBe('network')
    }
  })

  it('a non-ok response throws ApiError with the response status classified', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(jsonResponse(404, { code: 'COURSE_NOT_FOUND', message: 'x' })) as unknown as typeof fetch

    try {
      await apiFetch('/api/courses/999')
      expect.unreachable('expected apiFetch to throw')
    } catch (error) {
      expect(error).toBeInstanceOf(ApiError)
      expect((error as ApiError).normalized.classification).toBe('domain')
      expect((error as ApiError).normalized.code).toBe('COURSE_NOT_FOUND')
    }
  })

  it('a successful response resolves with the parsed JSON body', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(jsonResponse(200, { id: 1, title: '테스트 강좌' })) as unknown as typeof fetch

    const result = await apiFetch<{ id: number; title: string }>('/api/courses/1')
    expect(result).toEqual({ id: 1, title: '테스트 강좌' })
  })

  it('a 204 response resolves to undefined without attempting to parse a body', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(new Response(null, { status: 204 })) as unknown as typeof fetch

    const result = await apiFetch('/api/waitlist-entries/1', { method: 'DELETE', token: 't' })
    expect(result).toBeUndefined()
  })
})

describe('apiFetch — base URL join tolerates an empty base (design.md §B.2)', () => {
  const originalFetch = globalThis.fetch
  let fetchSpy: ReturnType<typeof vi.fn>

  beforeEach(() => {
    fetchSpy = vi.fn().mockResolvedValue(jsonResponse(200, {}))
    globalThis.fetch = fetchSpy as unknown as typeof fetch
  })

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('calls the path as-is when VITE_API_BASE_URL is unset in this test env', async () => {
    await apiFetch('/api/courses')
    const [url] = fetchSpy.mock.calls[0] as [string]
    expect(url.endsWith('/api/courses')).toBe(true)
  })
})

describe('apiFetch — 401 triggers the global session-expired notification (REQ-SES-007, M2)', () => {
  const originalFetch = globalThis.fetch

  afterEach(() => {
    globalThis.fetch = originalFetch
  })

  it('notifies subscribers when a response is classified session-expired (401, F2 body — no code field)', async () => {
    globalThis.fetch = vi
      .fn()
      .mockResolvedValue(jsonResponse(401, { timestamp: 't', status: 401, error: 'Unauthorized', path: '/x' })) as unknown as typeof fetch
    const listener = vi.fn()
    const unsubscribe = subscribeToSessionExpired(listener)
    try {
      await expect(apiFetch('/api/enrollments/mine', { token: 'expired' })).rejects.toBeInstanceOf(ApiError)
      expect(listener).toHaveBeenCalledTimes(1)
    } finally {
      unsubscribe()
    }
  })

  it('notifies subscribers for a 401 WITH a code field too (F1 INVALID_CREDENTIALS) — 401 always wins (plan.md §C.2)', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(jsonResponse(401, { code: 'INVALID_CREDENTIALS', message: 'bad creds' })) as unknown as typeof fetch
    const listener = vi.fn()
    const unsubscribe = subscribeToSessionExpired(listener)
    try {
      await expect(apiFetch('/api/auth/login', { method: 'POST' })).rejects.toBeInstanceOf(ApiError)
      expect(listener).toHaveBeenCalledTimes(1)
    } finally {
      unsubscribe()
    }
  })

  it('does not notify subscribers for a non-401 error', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(jsonResponse(404, { code: 'COURSE_NOT_FOUND', message: 'x' })) as unknown as typeof fetch
    const listener = vi.fn()
    const unsubscribe = subscribeToSessionExpired(listener)
    try {
      await expect(apiFetch('/api/courses/999')).rejects.toBeInstanceOf(ApiError)
      expect(listener).not.toHaveBeenCalled()
    } finally {
      unsubscribe()
    }
  })

  it('does not notify subscribers on success', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(jsonResponse(200, { ok: true })) as unknown as typeof fetch
    const listener = vi.fn()
    const unsubscribe = subscribeToSessionExpired(listener)
    try {
      await apiFetch('/api/courses')
      expect(listener).not.toHaveBeenCalled()
    } finally {
      unsubscribe()
    }
  })

  it('stops notifying after the returned unsubscribe function is called', async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(jsonResponse(401, {})) as unknown as typeof fetch
    const listener = vi.fn()
    const unsubscribe = subscribeToSessionExpired(listener)
    unsubscribe()

    await expect(apiFetch('/api/enrollments/mine', { token: 'expired' })).rejects.toBeInstanceOf(ApiError)
    expect(listener).not.toHaveBeenCalled()
  })
})
