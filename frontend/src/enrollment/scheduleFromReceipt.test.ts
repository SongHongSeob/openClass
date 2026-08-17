// @MX:NOTE: [AUTO] AC-FE-073a의 직접 테스트 — 보존된 접수 시각 기준으로
// 스케줄이 재계산됨을 확인한다(REQ-ENR-011). "마운트 직후와 동등한 상태"를
// receiptStorage에서 읽은 접수 시각 + 고정된 now로 재현한다 — 화면 마운트
// 시점이 아니라 최초 접수 시점부터 경과가 측정되어야 한다.

import { describe, expect, it } from 'vitest'
import { saveReceiptTimestamp, loadReceiptTimestamp, type ReceiptStorageLike } from './receiptStorage'
import { computePollingInterval } from './pollingSchedule'

function createMemoryStorage(): ReceiptStorageLike {
  const store = new Map<string, string>()
  return {
    getItem: (key) => store.get(key) ?? null,
    setItem: (key, value) => {
      store.set(key, value)
    },
  }
}

describe('AC-FE-073a: 새로고침/재마운트 후 스케줄 재계산', () => {
  it('접수 시각이 20초 전으로 보존되어 있으면 간격은 1초가 아니라 3초를 반환한다', () => {
    const storage = createMemoryStorage()
    const receivedAtMs = 1_700_000_000_000
    saveReceiptTimestamp(storage, 55, receivedAtMs)

    // 재마운트(새로고침) 시점 — 접수로부터 20초가 지난 "지금".
    const remountNowMs = receivedAtMs + 20_000

    const persisted = loadReceiptTimestamp(storage, 55)
    expect(persisted).toBe(receivedAtMs)

    const elapsedMs = remountNowMs - (persisted as number)
    expect(computePollingInterval(elapsedMs)).toBe(3000)
  })

  it('상한 중단은 계산 시점이 아니라 보존된 접수 시각으로부터 30초 시점에 지시된다', () => {
    const storage = createMemoryStorage()
    const receivedAtMs = 1_700_000_000_000
    saveReceiptTimestamp(storage, 56, receivedAtMs)

    // 재마운트 시점이 접수로부터 정확히 30초 후라면 즉시 중단이어야 한다 —
    // 재마운트 시점(0초 경과)부터 다시 세는 결함이 있다면 1초를 반환할 것이다.
    const remountNowMs = receivedAtMs + 30_000
    const persisted = loadReceiptTimestamp(storage, 56) as number
    const elapsedMs = remountNowMs - persisted

    expect(computePollingInterval(elapsedMs)).toBe('stop')
  })
})
