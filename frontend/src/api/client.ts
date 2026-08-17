// @MX:NOTE: [AUTO] 기준 주소 결합 + Authorization 부착 + 오류 정규화 3가지만
// 담당하는 얇은 fetch 래퍼(plan.md §C.6 — 단순성 사다리 3단계, 신규 의존성 없음).
// M1 대상 요구사항: REQ-NFR-003(기준 주소 환경 설정 주입), REQ-ERR-001~006(오류
// 정규화 단일 지점 소비).

import { normalizeFetchFailure, normalizeResponseError, type NormalizedError } from './errors'

/**
 * 백엔드가 소스에 하드코딩되지 않도록(REQ-NFR-003) 빌드 시점 환경 변수로
 * 주입한다. 미설정 시 빈 문자열 — 프론트엔드·백엔드를 동일 오리진에서 서빙하는
 * 배치는 이 경우 상대 경로 호출로 그대로 동작한다(design.md §B.2).
 */
const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

/** 백엔드가 CORS로 허용하는 헤더는 이 2종뿐이다(spec.md §A.5) — 그 외를 추가하지 않는다. */
export interface ApiRequestOptions {
  method?: 'GET' | 'POST' | 'PATCH' | 'DELETE'
  body?: unknown
  /** 인증이 필요한 호출에만 전달한다. 세션 소스(sessionStorage)는 M2가 배선한다. */
  token?: string
}

/** 정규화된 오류를 실어 던지는 예외. 화면은 `error.normalized`만 읽는다. */
export class ApiError extends Error {
  readonly normalized: NormalizedError

  constructor(normalized: NormalizedError) {
    super(normalized.message)
    this.name = 'ApiError'
    this.normalized = normalized
  }
}

/**
 * fetch 래퍼. 실패는 항상 {@link ApiError}로 던지며, 오류 정규화(REQ-ERR-002
 * 단일 지점)를 이 함수 하나만 통과한다 — 호출자는 원본 `Response`나 바디를
 * 직접 다루지 않는다.
 */
export async function apiFetch<T>(path: string, options: ApiRequestOptions = {}): Promise<T> {
  const { method = 'GET', body, token } = options

  const headers: Record<string, string> = {}
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response: Response
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
      // credentials를 명시하지 않는다 — 기본값(same-origin)을 유지한다.
      // 백엔드 allowCredentials=false이므로 'include'로 바꾸면 브라우저가
      // preflight 응답 자체를 거부한다(design.md §B.1).
    })
  } catch {
    // fetch가 reject됨 = 응답을 받지 못함(네트워크 단절·CORS 차단) — 판정 1단계.
    throw new ApiError(normalizeFetchFailure())
  }

  if (!response.ok) {
    const errorBody = await safeParseJson(response)
    throw new ApiError(normalizeResponseError(response.status, errorBody))
  }

  if (response.status === 204) {
    return undefined as T
  }

  return (await safeParseJson(response)) as T
}

/** 바디 파싱 실패(빈 본문·비-JSON)에서도 예외를 던지지 않는다(INV-FE-006). */
async function safeParseJson(response: Response): Promise<unknown> {
  try {
    return await response.json()
  } catch {
    return undefined
  }
}
