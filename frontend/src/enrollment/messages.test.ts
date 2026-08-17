// @MX:NOTE: [AUTO] 종단 결과 8종 안내 문구 매핑의 RED 테스트 — REQ-ENR-009.
// 8종 각각 서로 구별되는 문구를 가지며, 목록에 없는 미지 값은 일반 안내
// 문구로 대체된다(오류로 중단하지 않는다). AC-FE-067.

import { describe, expect, it } from 'vitest'
import { selectTerminalMessage } from './messages'

const KNOWN_TERMINALS = [
  'SUCCESS',
  'WAITLISTED',
  'CLOSED',
  'REJECTED',
  'FAILED',
  'CANCELLED',
  'PROMOTED',
  'NOOP',
] as const

describe('selectTerminalMessage', () => {
  it('AC-FE-067: 알려진 8종은 서로 다른 문구를 반환한다', () => {
    const messages = KNOWN_TERMINALS.map((status) => selectTerminalMessage(status))
    const uniqueMessages = new Set(messages)
    expect(uniqueMessages.size).toBe(KNOWN_TERMINALS.length)
  })

  it('알려진 8종 각각은 비어 있지 않은 문구를 반환한다', () => {
    for (const status of KNOWN_TERMINALS) {
      expect(selectTerminalMessage(status).length).toBeGreaterThan(0)
    }
  })

  it('REQ-ENR-009: 목록에 없는 미지 값은 일반 안내 문구로 대체된다 (오류 아님)', () => {
    const message = selectTerminalMessage('SOME_FUTURE_VALUE_NEVER_SEEN')
    expect(message.length).toBeGreaterThan(0)
    expect(KNOWN_TERMINALS.map((s) => selectTerminalMessage(s))).not.toContain(message)
  })
})
