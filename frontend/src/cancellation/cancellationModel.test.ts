// @MX:NOTE: [AUTO] 취소 화면 순수 로직의 RED 테스트 — plan.md §D.1 "목록
// 응답 → 화면 모델 변환" 부류. 과업 지시 Section D (a)~(e)를 직접 검증한다.

import { describe, expect, it } from 'vitest'
import {
  decidePostCancelAction,
  describeCancelError,
  formatWaitlistPositionLabel,
  resolveEnrollmentCancelTarget,
  resolveWaitlistCancelTarget,
  toListView,
} from './cancellationModel'
import { ApiError } from '../api/client'
import type { EnrollmentListItem, WaitlistListItem } from '../api/types'

function enrollmentItem(overrides: Partial<EnrollmentListItem> = {}): EnrollmentListItem {
  return {
    enrollmentId: 1,
    courseId: 10,
    courseTitle: '알고리즘 입문',
    status: 'SUCCESS',
    enrolledAt: '2026-08-01T00:00:00',
    ...overrides,
  }
}

function waitlistItem(overrides: Partial<WaitlistListItem> = {}): WaitlistListItem {
  return {
    waitlistEntryId: 99,
    courseId: 20,
    courseTitle: '자료구조',
    position: 1,
    status: 'PENDING',
    ...overrides,
  }
}

// (a) waitlistEntryId-vs-position 식별자 바인딩 — 잘못 배선되면 실패해야 한다.
describe('resolveWaitlistCancelTarget — REQ-CNL-003 / INV-FE-009', () => {
  it('waitlistEntryId를 반환하며 position을 반환하지 않는다', () => {
    const item = waitlistItem({ waitlistEntryId: 5, position: 999 })
    expect(resolveWaitlistCancelTarget(item)).toBe(5)
    expect(resolveWaitlistCancelTarget(item)).not.toBe(999)
  })
})

describe('resolveEnrollmentCancelTarget — REQ-CNL-001', () => {
  it('enrollmentId를 반환한다', () => {
    const item = enrollmentItem({ enrollmentId: 42 })
    expect(resolveEnrollmentCancelTarget(item)).toBe(42)
  })
})

// (b) 빈 배열 → "내역 없음" 상태, 오류 아님.
describe('toListView — REQ-CNL-007', () => {
  it('빈 배열은 empty 상태다 (오류 아님)', () => {
    expect(toListView([])).toEqual({ status: 'empty' })
  })

  it('항목이 있으면 items 상태다', () => {
    const items = [enrollmentItem()]
    expect(toListView(items)).toEqual({ status: 'items', items })
  })

  // (c) 클라이언트 재정렬 없음 — 응답 순서 그대로 보존.
  it('REQ-CNL-008: 응답 순서를 재정렬 없이 그대로 보존한다', () => {
    const items = [
      enrollmentItem({ enrollmentId: 30 }),
      enrollmentItem({ enrollmentId: 10 }),
      enrollmentItem({ enrollmentId: 20 }),
    ]
    const view = toListView(items)
    expect(view).toEqual({ status: 'items', items })
    if (view.status === 'items') {
      expect(view.items.map((item) => item.enrollmentId)).toEqual([30, 10, 20])
    }
  })
})

// (d) position은 항상 courseTitle과 나란히 — 단독 노출 금지.
describe('formatWaitlistPositionLabel — REQ-CNL-010 / INV-FE-011 / AC-FE-112', () => {
  it('courseTitle과 position을 모두 포함한다', () => {
    const item = waitlistItem({ courseTitle: '데이터베이스', position: 3 })
    const label = formatWaitlistPositionLabel(item)
    expect(label).toContain('데이터베이스')
    expect(label).toContain('3')
  })

  it('같은 position이라도 courseTitle이 다르면 서로 다른 문구를 만든다 (전역 순위 아님)', () => {
    const labelA = formatWaitlistPositionLabel(waitlistItem({ courseTitle: '강좌 A', position: 2 }))
    const labelB = formatWaitlistPositionLabel(waitlistItem({ courseTitle: '강좌 B', position: 2 }))
    expect(labelA).not.toBe(labelB)
  })

  it('전역 순위·승격 예정 순서를 뜻하는 표현을 쓰지 않는다', () => {
    const label = formatWaitlistPositionLabel(waitlistItem())
    expect(label).not.toContain('내 대기 순위')
    expect(label).not.toContain('승격 예정 순서')
  })
})

// (e) 취소 성공 후 재조회 — 로컬 스플라이스 아님.
describe('decidePostCancelAction — REQ-CNL-009', () => {
  it('항상 재조회를 지시한다', () => {
    expect(decidePostCancelAction()).toBe('refetch')
  })
})

describe('describeCancelError — REQ-CNL-005', () => {
  it('403과 404를 구별할 수 없는 동일 문구로 통합한다', () => {
    const forbidden = new ApiError({ classification: 'status-code', message: '이 작업을 수행할 권한이 없습니다.', status: 403 })
    const notFound = new ApiError({ classification: 'status-code', message: '요청한 대상을 찾을 수 없습니다.', status: 404 })
    expect(describeCancelError(forbidden)).toBe(describeCancelError(notFound))
  })

  it('403/404가 아닌 오류는 정규화된 문구를 그대로 사용한다', () => {
    const conflict = new ApiError({ classification: 'status-code', message: '요청을 처리할 수 없는 상태입니다.', status: 409 })
    expect(describeCancelError(conflict)).toBe('요청을 처리할 수 없는 상태입니다.')
  })
})
