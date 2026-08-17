import { describe, expect, it } from 'vitest'
import { computePageControls, isEnrollmentBlocked } from './catalogModel'
import type { CoursePage } from '../api/types'

function page(overrides: Partial<CoursePage>): CoursePage {
  return {
    items: [],
    totalElements: 0,
    totalPages: 0,
    currentPage: 0,
    ...overrides,
  }
}

describe('computePageControls — REQ-CAT-002 (서버 페이지 메타데이터 기반, 자체 분할 금지)', () => {
  it('AC-FE-041 관련: 0건이면 isEmpty=true이고 이전/다음 모두 비활성', () => {
    const controls = computePageControls(page({ totalElements: 0, totalPages: 0, currentPage: 0 }))
    expect(controls.isEmpty).toBe(true)
    expect(controls.hasPrevious).toBe(false)
    expect(controls.hasNext).toBe(false)
  })

  it('첫 페이지(currentPage=0)에서는 이전 페이지가 없다', () => {
    const controls = computePageControls(page({ totalElements: 45, totalPages: 3, currentPage: 0 }))
    expect(controls.isEmpty).toBe(false)
    expect(controls.hasPrevious).toBe(false)
    expect(controls.hasNext).toBe(true)
  })

  it('중간 페이지(currentPage=1, totalPages=3)에서는 이전·다음 모두 있다', () => {
    const controls = computePageControls(page({ totalElements: 45, totalPages: 3, currentPage: 1 }))
    expect(controls.hasPrevious).toBe(true)
    expect(controls.hasNext).toBe(true)
  })

  it('마지막 페이지(currentPage=2, totalPages=3, 0-인덱스)에서는 다음 페이지가 없다', () => {
    const controls = computePageControls(page({ totalElements: 45, totalPages: 3, currentPage: 2 }))
    expect(controls.hasPrevious).toBe(true)
    expect(controls.hasNext).toBe(false)
  })

  it('totalElements·totalPages·currentPage를 응답 그대로 보존한다 (자체 분할 금지)', () => {
    const controls = computePageControls(page({ totalElements: 45, totalPages: 3, currentPage: 1 }))
    expect(controls.totalElements).toBe(45)
    expect(controls.totalPages).toBe(3)
    expect(controls.currentPage).toBe(1)
  })
})

describe('isEnrollmentBlocked — REQ-CAT-005 (CLOSED 강좌는 수강신청 조작 미제공)', () => {
  it('CLOSED 강좌는 차단된다', () => {
    expect(isEnrollmentBlocked('CLOSED')).toBe(true)
  })

  it('OPEN 강좌는 차단되지 않는다', () => {
    expect(isEnrollmentBlocked('OPEN')).toBe(false)
  })

  it('알 수 없는 미지 상태값은 차단하지 않는다 (닫힌 화이트리스트 금지)', () => {
    expect(isEnrollmentBlocked('SOME_FUTURE_STATUS')).toBe(false)
  })
})
