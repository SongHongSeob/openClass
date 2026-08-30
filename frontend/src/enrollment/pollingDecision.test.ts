// @MX:NOTE: [AUTO] TanStack Query `refetchInterval`에 배선되는 순수 판정
// 로직의 RED 테스트 — REQ-ENR-010(네트워크 오류는 다음 주기 재시도, 401은
// 재시도하지 않고 세션 폐기로 넘김)의 비대칭을 검증한다. AC-FE-070·071.

import { describe, expect, it } from 'vitest'
import { decideNextPoll } from './pollingDecision'

describe('decideNextPoll', () => {
  it('AC-FE-070: 네트워크 오류 상태에서도 다음 주기 간격을 그대로 반환한다 (재시도)', () => {
    const result = decideNextPoll({
      status: undefined,
      errorClassification: 'network',
      elapsedMs: 2000,
    })
    expect(result).toBe(1000)
  })

  it('AC-FE-071: 401(session-expired)은 재시도하지 않고 폴링을 중단한다', () => {
    const result = decideNextPoll({
      status: undefined,
      errorClassification: 'session-expired',
      elapsedMs: 2000,
    })
    expect(result).toBe(false)
  })

  it('AC-FE-063/064: 종단 상태에 도달하면 폴링을 중단한다', () => {
    const result = decideNextPoll({
      status: 'SUCCESS',
      errorClassification: undefined,
      elapsedMs: 2000,
    })
    expect(result).toBe(false)
  })

  it('PENDING 상태이며 오류가 없으면 경과 시간 기준 스케줄을 따른다', () => {
    expect(
      decideNextPoll({ status: 'PENDING', errorClassification: undefined, elapsedMs: 1000 }),
    ).toBe(1000)
    expect(
      decideNextPoll({ status: 'PENDING', errorClassification: undefined, elapsedMs: 20000 }),
    ).toBe(3000)
  })

  it('AC-FE-062c: 상한(30초) 도달 시 PENDING이어도 중단을 지시한다', () => {
    const result = decideNextPoll({ status: 'PENDING', errorClassification: undefined, elapsedMs: 30000 })
    expect(result).toBe(false)
  })
})
