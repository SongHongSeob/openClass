// @MX:NOTE: [AUTO] 14개 엔드포인트 호출 함수의 성장 지점(design.md §A.8).
// M2는 인증 2종(signup·login)을 추가했고, M3은 공개 카탈로그 2종(목록·상세)을
// 추가한다 — 나머지 10개는 M4~M6이 채운다.

import { apiFetch } from './client'
import type { Course, CoursePage, LoginResult, SignupResult } from './types'

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
