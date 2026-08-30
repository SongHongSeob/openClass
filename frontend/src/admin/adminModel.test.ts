import { describe, expect, it } from 'vitest'
import {
  classifyCourseFormError,
  isCapacityIncrease,
  resolveAdminGuardFallback,
  shouldShowAdminMenu,
  toFormValues,
} from './adminModel'
import { ApiError } from '../api/client'
import type { Course } from '../api/types'

describe('shouldShowAdminMenu — REQ-ADM-001', () => {
  it('shows the admin entry point for an ADMIN role', () => {
    expect(shouldShowAdminMenu('ADMIN')).toBe(true)
  })

  it('hides the admin entry point for a MEMBER role', () => {
    expect(shouldShowAdminMenu('MEMBER')).toBe(false)
  })
})

describe('resolveAdminGuardFallback — REQ-ADM-002', () => {
  it('returns null (render children) when the guard allows', () => {
    expect(resolveAdminGuardFallback({ allowed: true })).toBeNull()
  })

  it('returns redirect-home when there is no session (login guidance, not a permission message)', () => {
    expect(resolveAdminGuardFallback({ allowed: false, reason: 'no-session' })).toBe('redirect-home')
  })

  it('returns forbidden when the session lacks the ADMIN role (not a redirect)', () => {
    expect(resolveAdminGuardFallback({ allowed: false, reason: 'insufficient-role' })).toBe('forbidden')
  })
})

const BASE_COURSE: Course = {
  id: 1,
  title: '알고리즘 스터디',
  description: '설명',
  capacity: 20,
  enrolledCount: 5,
  remainingCapacity: 15,
  startsAt: '2026-03-01T10:00:00',
  endsAt: '2026-03-01T12:00:00',
  status: 'OPEN',
}

describe('toFormValues — REQ-ADM-005 (edit form prefill, full-field carry-over)', () => {
  it('carries over every field the PATCH endpoint requires as current values', () => {
    expect(toFormValues(BASE_COURSE)).toEqual({
      title: '알고리즘 스터디',
      description: '설명',
      capacity: 20,
      startsAt: '2026-03-01T10:00:00',
      endsAt: '2026-03-01T12:00:00',
    })
  })
})

describe('isCapacityIncrease — REQ-ADM-006', () => {
  it('is true when the new capacity is greater than the current capacity', () => {
    expect(isCapacityIncrease(20, 30)).toBe(true)
  })

  it('is false when the new capacity equals the current capacity', () => {
    expect(isCapacityIncrease(20, 20)).toBe(false)
  })

  it('is false when the new capacity is smaller than the current capacity', () => {
    expect(isCapacityIncrease(20, 10)).toBe(false)
  })
})

describe('classifyCourseFormError — REQ-ADM-007', () => {
  it('highlights the capacity field for a 409 CAPACITY_BELOW_ENROLLMENT error', () => {
    const error = new ApiError({
      classification: 'domain',
      message: '현재 확정 인원보다 적은 정원으로는 변경할 수 없습니다.',
      code: 'CAPACITY_BELOW_ENROLLMENT',
      status: 409,
    })
    expect(classifyCourseFormError(error)).toEqual({
      message: '현재 확정 인원보다 적은 정원으로는 변경할 수 없습니다.',
      field: 'capacity',
    })
  })

  it('does not highlight any field for an unrelated domain error', () => {
    const error = new ApiError({
      classification: 'domain',
      message: '존재하지 않는 강좌입니다.',
      code: 'COURSE_NOT_FOUND',
      status: 404,
    })
    expect(classifyCourseFormError(error)).toEqual({
      message: '존재하지 않는 강좌입니다.',
      field: null,
    })
  })

  it('falls back to a generic message for a non-ApiError', () => {
    expect(classifyCourseFormError(new Error('boom'))).toEqual({
      message: '요청을 처리하는 중 오류가 발생했습니다.',
      field: null,
    })
  })
})
