// @MX:NOTE: [AUTO] 14개 엔드포인트 호출 함수의 성장 지점(design.md §A.8).
// M2는 인증 2종(signup·login)을 추가했고, M3은 공개 카탈로그 2종(목록·상세)을
// 추가했다. M4는 수강신청 접수(#5)·상태 조회(#6) 2종을 추가했다. M5는 관리자
// 3종(생성·수정·마감, #9~11)을 추가했다. M6이 나머지 4종(내 목록 조회 2종
// #13·14, 취소 2종 #7·8)을 채워 14개 전수를 완성한다.

import { apiFetch } from './client'
import type {
  Course,
  CoursePage,
  EnrollmentListItem,
  LoginResult,
  Member,
  MemberRole,
  Receipt,
  RequestStatus,
  SignupResult,
  WaitlistEntryId,
  WaitlistListItem,
} from './types'

export interface SignupPayload {
  email: string
  password: string
}

export interface LoginPayload {
  email: string
  password: string
}

/** `POST /api/auth/signup` — REQ-SES-001, spec.md §A.4 1번. */
export function signup(payload: SignupPayload): Promise<SignupResult> {
  return apiFetch<SignupResult>('/api/auth/signup', { method: 'POST', body: payload })
}

/** `POST /api/auth/login` — REQ-SES-002, spec.md §A.4 2번. */
export function login(payload: LoginPayload): Promise<LoginResult> {
  return apiFetch<LoginResult>('/api/auth/login', { method: 'POST', body: payload })
}

/**
 * `GET /api/courses?page&size&keyword` — REQ-CAT-001·002·007, spec.md §A.4
 * 3번(SPEC-COURSE-001 Amendment 1로 `keyword` 추가). 공개 엔드포인트이므로
 * `token`을 전달하지 않는다(REQ-CAT-006). `page`는 백엔드와 동일하게
 * 0-인덱스다(`CourseController.list` 기본값 `page=0`). `keyword`가
 * 비어있거나 공백뿐이면 파라미터 자체를 붙이지 않는다 — 기존 호출자(관리자
 * 목록 등)는 이 인자를 생략해도 동작이 그대로다(추가 전용 변경).
 */
export function getCourses(page: number, size: number, keyword?: string): Promise<CoursePage> {
  const trimmed = keyword?.trim()
  const keywordParam = trimmed ? `&keyword=${encodeURIComponent(trimmed)}` : ''
  return apiFetch<CoursePage>(`/api/courses?page=${page}&size=${size}${keywordParam}`)
}

/** `GET /api/courses/{id}` — REQ-CAT-004, spec.md §A.4 4번. 공개 엔드포인트. */
export function getCourseDetail(id: number): Promise<Course> {
  return apiFetch<Course>(`/api/courses/${id}`)
}

/**
 * `POST /api/courses/{courseId}/enrollments` — REQ-ENR-001, spec.md §A.4 5번.
 * 202(Accepted) — 큐 적재일 뿐 확정이 아니다(REQ-ENR-002). 응답의
 * `requestId`로 상태 폴링을 개시한다.
 */
export function submitEnrollment(courseId: number, token: string): Promise<Receipt> {
  return apiFetch<Receipt>(`/api/courses/${courseId}/enrollments`, { method: 'POST', token })
}

/**
 * `GET /api/enrollment-requests/{requestId}` — REQ-ENR-003, spec.md §A.4 6번.
 * 인증(본인) 엔드포인트 — 폴링 훅(`enrollment/useRequestStatus.ts`)이 이
 * 함수만을 통해 상태를 조회한다.
 */
export function getEnrollmentRequestStatus(requestId: number, token: string): Promise<RequestStatus> {
  return apiFetch<RequestStatus>(`/api/enrollment-requests/${requestId}`, { token })
}

/**
 * 관리자 강좌 생성·수정 요청 바디(`CourseCreateRequest`/`CourseUpdateRequest`
 * — 두 DTO는 필드 구성이 동일하다). `description`만 선택이며 나머지는 백엔드가
 * 전부 필수로 요구한다(spec.md §A.4 주의 사항, REQ-ADM-005).
 */
export interface CourseFormPayload {
  title: string
  description?: string
  capacity: number
  startsAt: string
  endsAt: string
}

/** `POST /api/admin/courses` — REQ-ADM-004, spec.md §A.4 9번. */
export function createCourse(payload: CourseFormPayload, token: string): Promise<Course> {
  return apiFetch<Course>('/api/admin/courses', { method: 'POST', body: payload, token })
}

/**
 * `PATCH /api/admin/courses/{id}` — REQ-ADM-005, spec.md §A.4 10번. `PATCH`
 * 이지만 요청 본문 필드가 전부 필수이므로(§A.4 주의 사항), 호출자는 현재 값
 * 전체를 채운 `payload`를 전달해야 한다 — 변경 필드만 보내면 400이 반환된다.
 */
export function updateCourse(id: number, payload: CourseFormPayload, token: string): Promise<Course> {
  return apiFetch<Course>(`/api/admin/courses/${id}`, { method: 'PATCH', body: payload, token })
}

/**
 * `POST /api/admin/courses/{id}/close` — REQ-ADM-008, spec.md §A.4 11번.
 * 물리 삭제가 아니라 마감 전이다 — `DELETE /api/admin/courses/{id}`(12번)도
 * 내부적으로 동일하게 처리되지만, "삭제" 표기 오해를 피하기 위해(REQ-ADM-009)
 * 이 화면은 `/close`를 호출한다.
 */
export function closeCourse(id: number, token: string): Promise<Course> {
  return apiFetch<Course>(`/api/admin/courses/${id}/close`, { method: 'POST', token })
}

/**
 * `GET /api/enrollments/mine` — REQ-CNL-006, spec.md §A.4 13번. 회원 식별자를
 * 질의 파라미터·경로 변수·본문 어디에도 싣지 않는다 — 인증 주체에서만
 * 범위가 유도된다. 0건이면 `200` + `[]`(REQ-CNL-007, 404 아님).
 */
export function getMyEnrollments(token: string): Promise<EnrollmentListItem[]> {
  return apiFetch<EnrollmentListItem[]>('/api/enrollments/mine', { token })
}

/**
 * `GET /api/waitlist-entries/mine` — REQ-CNL-006, spec.md §A.4 14번. 13번과
 * 동일하게 회원 식별자를 신지 않는다.
 */
export function getMyWaitlistEntries(token: string): Promise<WaitlistListItem[]> {
  return apiFetch<WaitlistListItem[]>('/api/waitlist-entries/mine', { token })
}

/**
 * `DELETE /api/enrollments/{enrollmentId}` — REQ-CNL-001, spec.md §A.4 7번.
 * 202(Accepted) — 취소도 큐를 경유하므로 확정이 아니다(REQ-CNL-002).
 * `enrollmentId`는 반드시 {@link getMyEnrollments} 응답에서만 유래해야 한다
 * (`cancellation/cancellationModel.ts`의 `resolveEnrollmentCancelTarget`).
 */
export function cancelEnrollment(enrollmentId: number, token: string): Promise<Receipt> {
  return apiFetch<Receipt>(`/api/enrollments/${enrollmentId}`, { method: 'DELETE', token })
}

/**
 * `DELETE /api/waitlist-entries/{entryId}` — REQ-CNL-003, spec.md §A.4 8번.
 * 200 동기 응답(본문 없음) — 확정 취소와 달리 폴링을 개시하지 않는다.
 * 경로 변수는 반드시 `waitlistEntryId`이며 `position`이 아니다(INV-FE-009,
 * `cancellation/cancellationModel.ts`의 `resolveWaitlistCancelTarget`). 인자
 * 타입이 `WaitlistEntryId`(브랜디드 타입)이므로 `position`(순수 `number`)을
 * 실수로 넘기는 호출은 타입 검사에서 거부된다(AC-FE-109).
 */
export function cancelWaitlistEntry(waitlistEntryId: WaitlistEntryId, token: string): Promise<void> {
  return apiFetch<void>(`/api/waitlist-entries/${waitlistEntryId}`, { method: 'DELETE', token })
}

/** `GET /api/admin/members` — 관리자 회원 관리 화면의 전체 회원 목록 조회. */
export function listMembers(token: string): Promise<Member[]> {
  return apiFetch<Member[]>('/api/admin/members', { token })
}

/** `PATCH /api/admin/members/{memberId}/role` — 회원 역할 승격/강등. */
export function updateMemberRole(memberId: number, role: MemberRole, token: string): Promise<Member> {
  return apiFetch<Member>(`/api/admin/members/${memberId}/role`, { method: 'PATCH', body: { role }, token })
}
