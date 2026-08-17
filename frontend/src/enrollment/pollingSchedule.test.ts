// @MX:NOTE: [AUTO] 폴링 스케줄 계산 + 종단 판정의 RED 테스트 — plan.md §C.3 /
// design.md §A.4의 표를 직접 검증한다. AC-FE-062a/b/c(간격 경계) ·
// AC-FE-063(종단 판정 = PENDING의 여집합, 미지 문자열 포함).

import { describe, expect, it } from 'vitest'
import { computePollingInterval, isTerminalStatus } from './pollingSchedule'

describe('computePollingInterval', () => {
  it('AC-FE-062a: 0~5초 구간은 1초 간격을 반환한다', () => {
    expect(computePollingInterval(0)).toBe(1000)
    expect(computePollingInterval(4999)).toBe(1000)
  })

  it('AC-FE-062b: 5~15초 구간은 2초 간격을 반환한다', () => {
    expect(computePollingInterval(5000)).toBe(2000)
    expect(computePollingInterval(14999)).toBe(2000)
  })

  it('AC-FE-062b: 15~30초 구간은 3초 간격을 반환한다', () => {
    expect(computePollingInterval(15000)).toBe(3000)
    expect(computePollingInterval(29999)).toBe(3000)
  })

  it('AC-FE-062c / AC-FE-073a: 30초 시점부터는 중단을 지시한다', () => {
    expect(computePollingInterval(30000)).toBe('stop')
    expect(computePollingInterval(60000)).toBe('stop')
  })
})

describe('isTerminalStatus', () => {
  it('AC-FE-063: PENDING은 종단이 아니다', () => {
    expect(isTerminalStatus('PENDING')).toBe(false)
  })

  it('AC-FE-063: 알려진 8종 종단 값은 모두 종단으로 판정된다', () => {
    const knownTerminals = [
      'SUCCESS',
      'WAITLISTED',
      'CLOSED',
      'REJECTED',
      'FAILED',
      'CANCELLED',
      'PROMOTED',
      'NOOP',
    ]
    for (const status of knownTerminals) {
      expect(isTerminalStatus(status)).toBe(true)
    }
  })

  it('AC-FE-063: 미지의 문자열도 종단으로 판정된다 (화이트리스트 금지)', () => {
    expect(isTerminalStatus('SOME_FUTURE_VALUE_NEVER_SEEN')).toBe(true)
  })
})
