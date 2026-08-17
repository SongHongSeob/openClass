// @MX:ANCHOR: [AUTO] 취소 화면(내 수강신청·내 대기명단) 순수 로직 지점 —
// 목록 응답 → 화면 모델 변환, 취소 대상 식별자 판정, 오류 문구 통합을
// 화면 컴포넌트에서 분리했다(plan.md §D.1 "화면 모델 변환" 부류). M6 대상
// 요구사항: REQ-CNL-001·003·005·007~010, INV-FE-009~011.
// @MX:REASON: `waitlistEntryId`와 `position`은 둘 다 `Long`이라 타입 검사가
// 혼동을 잡아 주지 못한다(spec.md §A.4) — 취소 대상 판정을 이 모듈 하나로
// 모아 두면, 화면이 직접 `item.position`을 취소 호출에 넘기는 경로 자체가
// 생기지 않는다.

import { ApiError } from '../api/client'
import type { EnrollmentListItem, WaitlistListItem } from '../api/types'

/**
 * REQ-CNL-007 — 빈 배열(`200` + `[]`)은 오류가 아니라 "보유 내역 없음"이다.
 * REQ-CNL-008 — 응답 순서를 그대로 보존한다. 이 함수는 정렬을 전혀 수행하지
 * 않으므로(정렬 호출 없음), 호출자가 넘긴 배열 순서가 그대로 `items`에
 * 남는다 — 화면은 이 결과를 재정렬 없이 그대로 렌더링해야 한다.
 */
export type ListView<T> = { status: 'empty' } | { status: 'items'; items: T[] }

export function toListView<T>(items: T[]): ListView<T> {
  return items.length === 0 ? { status: 'empty' } : { status: 'items', items }
}

/**
 * REQ-CNL-001 — 확정 수강신청 취소 대상 식별자는 `GET /api/enrollments/mine`
 * 응답의 `enrollmentId`에서만 유래한다.
 */
export function resolveEnrollmentCancelTarget(item: EnrollmentListItem): number {
  return item.enrollmentId
}

/**
 * REQ-CNL-003 / INV-FE-009 — 대기명단 취소 대상 식별자는 `waitlistEntryId`이며
 * `position`이 아니다. 화면은 이 함수의 반환값만 취소 API에 전달해야 한다 —
 * `item.position`을 직접 전달하는 코드 경로를 만들지 않는다.
 */
export function resolveWaitlistCancelTarget(item: WaitlistListItem): number {
  return item.waitlistEntryId
}

/**
 * REQ-CNL-010 / INV-FE-011 / AC-FE-112 — `position`은 반드시 `courseTitle`과
 * 나란히 표시하며, 전역 대기 순위·승격 예정 순서로 읽히는 표현을 쓰지 않는다.
 * 화면은 `position`을 단독으로 렌더링하지 않고 이 함수가 만든 문자열만
 * 사용한다 — 이 문자열은 강좌 단위 순번임을 명시한다.
 */
export function formatWaitlistPositionLabel(item: WaitlistListItem): string {
  return `${item.courseTitle} — 이 강좌 내 대기 ${item.position}번째 (다른 강좌의 대기와는 무관한 강좌별 순번)`
}

/**
 * REQ-CNL-009 — 취소 성공 후 표시하는 목록은 해당 조회 API를 재호출하여 얻은
 * 결과여야 하며, 클라이언트가 보유하던 이전 목록에서 항목을 임의로 제거한
 * 결과를 표시해서는 안 된다. 이 함수는 취소 성공 시 화면이 취해야 할 유일한
 * 행동을 판정하며, 항상 재조회를 반환한다 — 로컬 제거를 지시하는 값은
 * 존재하지 않는다.
 */
export type PostCancelAction = 'refetch'

export function decidePostCancelAction(): PostCancelAction {
  return 'refetch'
}

const CANCEL_OWNERSHIP_MESSAGE = '취소 대상을 찾을 수 없거나 본인 소유가 아닙니다.'
const CANCEL_FALLBACK_MESSAGE = '취소 처리 중 오류가 발생했습니다.'

/**
 * REQ-CNL-005 — 취소 요청이 403 또는 404를 반환하면, 두 응답을 구별하지 않고
 * 동일한 안내로 표시한다(소유자 존재 여부를 추측할 수 있는 정보를 노출하지
 * 않는다). `errors.ts`의 상태 코드별 문구(STATUS_MESSAGES)는 403과 404를
 * 서로 다르게 안내하므로, 취소 화면은 그 문구를 그대로 쓰지 않고 이 함수가
 * 만든 통합 문구를 사용한다.
 */
export function describeCancelError(error: unknown): string {
  if (error instanceof ApiError) {
    const status = error.normalized.status
    if (status === 403 || status === 404) {
      return CANCEL_OWNERSHIP_MESSAGE
    }
    return error.normalized.message
  }
  return CANCEL_FALLBACK_MESSAGE
}
