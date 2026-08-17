// @MX:NOTE: [AUTO] 접수 시각 영속 지점의 RED 테스트 — REQ-ENR-011. requestId를
// 키로 sessionStorage에 접수 시각(ms)을 기록·복원한다. tokenStorage.ts와
// 동일한 TokenStorageLike 스타일 인터페이스로 jsdom 없이 인메모리 목을 쓴다.

import { describe, expect, it } from 'vitest'
import { loadReceiptTimestamp, saveReceiptTimestamp, type ReceiptStorageLike } from './receiptStorage'

function createMemoryStorage(): ReceiptStorageLike {
  const store = new Map<string, string>()
  return {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => {
      store.set(key, value)
    },
  }
}

describe('receiptStorage', () => {
  it('저장한 접수 시각을 동일 requestId로 복원한다', () => {
    const storage = createMemoryStorage()
    saveReceiptTimestamp(storage, 42, 1_700_000_000_000)
    expect(loadReceiptTimestamp(storage, 42)).toBe(1_700_000_000_000)
  })

  it('저장된 적 없는 requestId는 null을 반환한다', () => {
    const storage = createMemoryStorage()
    expect(loadReceiptTimestamp(storage, 999)).toBeNull()
  })

  it('서로 다른 requestId는 서로 다른 키로 격리된다', () => {
    const storage = createMemoryStorage()
    saveReceiptTimestamp(storage, 1, 100)
    saveReceiptTimestamp(storage, 2, 200)
    expect(loadReceiptTimestamp(storage, 1)).toBe(100)
    expect(loadReceiptTimestamp(storage, 2)).toBe(200)
  })

  it('손상된 값(숫자가 아님)이 저장되어 있으면 null을 반환한다 (예외를 던지지 않는다)', () => {
    const storage = createMemoryStorage()
    storage.setItem('openclass.enrollment.receiptAt.7', 'not-a-number')
    expect(loadReceiptTimestamp(storage, 7)).toBeNull()
  })
})
