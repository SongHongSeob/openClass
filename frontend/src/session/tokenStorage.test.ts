import { describe, expect, it } from 'vitest'
import { clearToken, loadToken, saveToken, type TokenStorageLike } from './tokenStorage'

/** jsdom 없이 `Storage`를 대체하는 인메모리 목. */
function createMemoryStorage(): TokenStorageLike {
  const store = new Map<string, string>()
  return {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => {
      store.set(key, value)
    },
    removeItem: (key) => {
      store.delete(key)
    },
  }
}

describe('tokenStorage — REQ-SES-004 sessionStorage 연동', () => {
  it('loadToken returns null before anything is saved', () => {
    const storage = createMemoryStorage()
    expect(loadToken(storage)).toBeNull()
  })

  it('round-trips a saved token through loadToken', () => {
    const storage = createMemoryStorage()
    saveToken(storage, 'abc.def.ghi')
    expect(loadToken(storage)).toBe('abc.def.ghi')
  })

  it('clearToken removes the token so loadToken returns null again', () => {
    const storage = createMemoryStorage()
    saveToken(storage, 'abc.def.ghi')
    clearToken(storage)
    expect(loadToken(storage)).toBeNull()
  })

  it('saveToken overwrites a previously stored token', () => {
    const storage = createMemoryStorage()
    saveToken(storage, 'first.token.value')
    saveToken(storage, 'second.token.value')
    expect(loadToken(storage)).toBe('second.token.value')
  })

  it('does not touch unrelated keys in the same storage', () => {
    const storage = createMemoryStorage()
    storage.setItem('unrelated-key', 'untouched')
    saveToken(storage, 'abc.def.ghi')
    clearToken(storage)
    expect(storage.getItem('unrelated-key')).toBe('untouched')
  })
})
