// @MX:NOTE: [AUTO] 14개 엔드포인트 호출 함수의 성장 지점(design.md §A.8).
// M2는 인증 2종(signup·login)을 추가했고, M3은 공개 카탈로그 2종(목록·상세)을
// 추가했다. M4는 수강신청 접수(#5)·상태 조회(#6) 2종을 추가한다 — 나머지
// 8개(취소·관리자)는 M5~M6이 채운다.

import { apiFetch } from './client'
import type { Course, CoursePage, LoginResult, Receipt, RequestStatus, SignupResult } from './types'

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
 * `GET /api/courses?page&size` — REQ-CAT-001·002, spec.md §A.4 3번. 공개
 * 엔드포인트이므로 `token`을 전달하지 않는다(REQ-CAT-006). `page`는
 * 백엔드와 동일하게 0-인덱스다(`CourseController.list` 기본값 `page=0`).
 */
export function getCourses(page: number, size: number): Promise<CoursePage> {
  return apiFetch<CoursePage>(`/api/courses?page=${page}&size=${size}`)
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
