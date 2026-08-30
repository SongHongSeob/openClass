// @MX:NOTE: [AUTO] 관리자 화면(M5)의 순수 로직 지점 — 역할 가드 소비, 정원
// 증설 판정, 폼 프리필, 오류 분류를 화면 컴포넌트에서 분리했다(plan.md §D.1
// "화면 모델 변환" 부류). 역할 판정 자체는 새로 만들지 않고 M2가 만든
// `evaluateRoleGuard`(../routing/guardLogic.ts)를 그대로 소비한다
// (REQ-ADM-001~003, spec.md §A.7).

import { ApiError } from '../api/client'
import type { CourseFormPayload } from '../api/endpoints'
import type { GuardResult } from '../routing/guardLogic'
import type { Course, MemberRole } from '../api/types'

/** REQ-ADM-001 — 관리자 화면 진입 수단은 role === 'ADMIN'일 때만 노출한다. */
export function shouldShowAdminMenu(role: MemberRole): boolean {
  return role === 'ADMIN'
}

/**
 * REQ-ADM-002 — `evaluateRoleGuard`의 판정 결과를 라우트 수준의 대응으로
 * 변환한다. 세션 자체가 없으면(`no-session`) 로그인 유도가 맞지만, 세션은
 * 있으나 역할이 부족하면(`insufficient-role`) 화면을 렌더링하지 않고 권한
 * 없음만 안내한다 — 두 경로는 서로 다른 안내다(design.md §A.6, AC-FE-082).
 */
export type AdminRouteFallback = 'redirect-home' | 'forbidden'

export function resolveAdminGuardFallback(result: GuardResult): AdminRouteFallback | null {
  if (result.allowed) {
    return null
  }
  return result.reason === 'no-session' ? 'redirect-home' : 'forbidden'
}

/**
 * REQ-ADM-005 — 강좌 상세 응답을 수정 폼의 초기값으로 변환한다. 백엔드가
 * `PATCH`임에도 전 필드를 필수로 요구하므로, 폼은 이 값 전체를 유지하다가
 * 그대로 다시 제출한다(plan.md AP-8 — 변경 필드만 보내면 400).
 */
export function toFormValues(course: Course): CourseFormPayload {
  return {
    title: course.title,
    description: course.description,
    capacity: course.capacity,
    startsAt: course.startsAt,
    endsAt: course.endsAt,
  }
}

/**
 * REQ-ADM-006 — 새 정원이 현재 정원보다 클 때만 대기자 승격 안내 대상이다.
 * 동일·감소는 안내 대상이 아니다(감소는 REQ-ADM-007의 409 처리 대상).
 */
export function isCapacityIncrease(currentCapacity: number, nextCapacity: number): boolean {
  return nextCapacity > currentCapacity
}

export interface CourseFormError {
  message: string
  /** 409(`CAPACITY_BELOW_ENROLLMENT`)일 때만 정원 필드를 지목한다. */
  field: 'capacity' | null
}

const FALLBACK_MESSAGE = '요청을 처리하는 중 오류가 발생했습니다.'

/**
 * REQ-ADM-007 — 정원 축소 제출이 409(`CAPACITY_BELOW_ENROLLMENT`)로 거부되면
 * 정원 필드를 지목한 안내로 분류한다. 안내 문구 자체는 `errors.ts`
 * (REQ-ERR-002 오류 정규화 단일 지점)가 이미 판정한 값을 그대로 사용한다 —
 * 이 함수는 그 결과를 "정원 필드 강조 여부"로 재분류할 뿐, 문구를 새로
 * 만들지 않는다(오류 정규화 단일 지점 유지).
 */
export function classifyCourseFormError(error: unknown): CourseFormError {
  if (error instanceof ApiError) {
    const field = error.normalized.code === 'CAPACITY_BELOW_ENROLLMENT' ? 'capacity' : null
    return { message: error.normalized.message, field }
  }
  return { message: FALLBACK_MESSAGE, field: null }
}
