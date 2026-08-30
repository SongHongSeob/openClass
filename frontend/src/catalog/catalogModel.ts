// @MX:NOTE: [AUTO] 강좌 카탈로그 화면의 순수 로직 지점 — 페이지 이동 계산과
// CLOSED 상태 판정을 화면 컴포넌트에서 분리했다(REQ-CAT-002, REQ-CAT-005).
// plan.md §D.1이 자동 테스트 대상으로 지정한 "화면 모델 변환" 부류에 속한다.

import type { Course, CoursePage } from '../api/types'

/**
 * `GET /api/courses` 응답의 페이지 메타데이터로부터 유도한 화면 표시용 값.
 * `totalElements`·`totalPages`·`currentPage`는 응답 원본을 그대로 보존한다 —
 * 클라이언트가 전체 목록을 받아 자체 분할하지 않는다(REQ-CAT-002).
 */
export interface PageControls {
  currentPage: number
  totalPages: number
  totalElements: number
  /** 보유 강좌가 0건인 정상 상태 — 오류가 아니다. */
  isEmpty: boolean
  hasPrevious: boolean
  hasNext: boolean
}

/**
 * `currentPage`는 백엔드(Spring Data `Page.getNumber()`)와 동일하게
 * 0-인덱스다(`CourseController.list` 기본값 `page=0`).
 */
export function computePageControls(page: CoursePage): PageControls {
  return {
    currentPage: page.currentPage,
    totalPages: page.totalPages,
    totalElements: page.totalElements,
    isEmpty: page.totalElements === 0,
    hasPrevious: page.currentPage > 0,
    hasNext: page.currentPage + 1 < page.totalPages,
  }
}

/**
 * REQ-CAT-005 — 모집 상태가 `CLOSED`인 동안 수강신청 조작을 제공하지 않는다.
 * M4가 실제 신청 CTA를 배선할 때 이 헬퍼를 재사용한다(과업 지시 — 재사용
 * 가능한 조각으로 유지). 미지 상태값은 차단하지 않는다 — `CourseStatus`가
 * 열린 유니온이므로 닫힌 화이트리스트로 판정하면 안 된다.
 */
export function isEnrollmentBlocked(status: Course['status']): boolean {
  return status === 'CLOSED'
}
