import { describe, expect, it } from 'vitest'
import type { EnrollmentStatus, CourseStatus, WaitlistListItem, EnrollmentListItem } from './types'

describe('EnrollmentStatus — REQ-ENR-009 type tolerance', () => {
  it('accepts every currently-known terminal status without a compile error', () => {
    const known: EnrollmentStatus[] = [
      'PENDING',
      'SUCCESS',
      'WAITLISTED',
      'CLOSED',
      'REJECTED',
      'FAILED',
      'CANCELLED',
      'PROMOTED',
      'NOOP',
    ]
    expect(known).toHaveLength(9)
  })

  it('accepts an unknown future status string (REQ-ENR-009 — must not be a closed union)', () => {
    // If EnrollmentStatus were narrowed back to a closed literal union, this
    // assignment fails to compile (tsc --noEmit) — the regression guard is at
    // the type level, not the runtime level.
    const futureStatus: EnrollmentStatus = 'SOME_FUTURE_STATUS_ADDED_BY_BACKEND'
    expect(typeof futureStatus).toBe('string')
  })
})

describe('CourseStatus — tolerant status union', () => {
  it('accepts an unknown future course status string', () => {
    const futureStatus: CourseStatus = 'SOME_FUTURE_COURSE_STATUS'
    expect(typeof futureStatus).toBe('string')
  })
})

describe('waitlistEntryId / position structural distinction (design.md §A.1, INV-FE-009)', () => {
  it('keeps waitlistEntryId and position as separate fields on the same item', () => {
    const item: WaitlistListItem = {
      waitlistEntryId: 1,
      courseId: 10,
      courseTitle: '테스트 강좌',
      position: 2,
      status: 'WAITING',
    }
    // The two identifiers must never collapse into a single field name — the
    // cancel call (M6) reads waitlistEntryId, never position.
    expect(item.waitlistEntryId).not.toBe(item.position)
  })
})

describe('EnrollmentListItem — dates stay strings (design.md §A.1 date rule)', () => {
  it('does not eagerly convert enrolledAt to a Date', () => {
    const item: EnrollmentListItem = {
      enrollmentId: 1,
      courseId: 10,
      courseTitle: '테스트 강좌',
      status: 'ENROLLED',
      enrolledAt: '2026-08-17T09:00:00',
    }
    expect(typeof item.enrolledAt).toBe('string')
  })
})
