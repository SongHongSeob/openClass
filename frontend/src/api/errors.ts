// @MX:ANCHOR: [AUTO] 오류 정규화 — 백엔드 오류 응답 3종(F1/F2/F3)을 단일 형태로
// 변환하는 유일한 지점(REQ-ERR-002). 화면 컴포넌트는 오류 바디를 직접 해석하지
// 않고 이 모듈의 결과만 소비한다.
// @MX:REASON: plan.md §C.2 — 판정 순서 자체가 계약이다. 401을 code 필드 판정보다
// 먼저 검사해야 두 401 발생 경로(로그인 실패·토큰 만료)가 모두 세션 폐기로
// 수렴한다. 순서를 바꾸면 REQ-SES-007이 한쪽 경로에서만 동작한다.

/** 정규화 결과의 분류. 화면은 이 값만으로 분기하며 원문 바디를 보지 않는다. */
export type ErrorClassification = 'network' | 'session-expired' | 'domain' | 'status-code'

export interface NormalizedError {
  classification: ErrorClassification
  message: string
  /** F1(도메인 오류) 응답의 `code` 필드. 없으면 undefined. */
  code?: string
  /** fetch 자체가 reject된 네트워크 오류에는 status가 없다. */
  status?: number
}

/** F1(도메인 오류) 백엔드 코드 → 안내 문구. research.md §4.1의 8종 전수. */
const CODE_MESSAGES: Record<string, string> = {
  DUPLICATE_EMAIL: '이미 가입된 이메일입니다.',
  INVALID_CREDENTIALS: '이메일 또는 비밀번호가 올바르지 않습니다.',
  COURSE_NOT_FOUND: '존재하지 않는 강좌입니다.',
  CAPACITY_BELOW_ENROLLMENT: '현재 확정 인원보다 적은 정원으로는 변경할 수 없습니다.',
  ENROLLMENT_REQUEST_NOT_FOUND: '요청을 찾을 수 없습니다.',
  ENROLLMENT_NOT_FOUND: '수강신청 내역을 찾을 수 없습니다.',
  WAITLIST_ENTRY_NOT_FOUND: '대기명단 항목을 찾을 수 없습니다.',
  INVALID_COURSE_ID: '유효하지 않은 강좌 식별자입니다.',
}

/** F2/F3(code 필드 없음) — HTTP 상태 코드 기준 안내 문구. */
const STATUS_MESSAGES: Record<number, string> = {
  400: '요청 내용을 다시 확인해 주세요.',
  401: '세션이 만료되었습니다. 다시 로그인해 주세요.',
  403: '이 작업을 수행할 권한이 없습니다.',
  404: '요청한 대상을 찾을 수 없습니다.',
  409: '요청을 처리할 수 없는 상태입니다.',
}

const FALLBACK_MESSAGE = '요청을 처리하는 중 오류가 발생했습니다.'
const NETWORK_MESSAGE = '서버에 연결할 수 없습니다. 네트워크 상태 또는 개발 서버 설정을 확인해 주세요.'

/**
 * 요청이 응답을 받지 못한 경우(fetch 자체가 reject) — 판정 순서 1단계.
 * REQ-ERR-005: 이 분류는 "서버 오류"로 표시하지 않는다.
 */
export function normalizeFetchFailure(): NormalizedError {
  return {
    classification: 'network',
    message: NETWORK_MESSAGE,
  }
}

/**
 * 응답은 받았으나 실패(non-2xx)한 경우의 정규화. 판정 순서(plan.md §C.2)를
 * 그대로 구현한다 — 2단계(401)가 3단계(code 필드)보다 앞선다.
 */
export function normalizeResponseError(status: number, body: unknown): NormalizedError {
  const code = extractErrorCode(body)

  // 2단계: 401은 바디 형태와 무관하게 세션 만료로 앞당겨 판정한다(REQ-SES-007).
  // code가 있어도(F1 INVALID_CREDENTIALS) 분류는 session-expired로 고정하되,
  // 문구는 code가 있으면 code 기준으로 선택한다 — 로그인 실패와 순수 만료가
  // 서로 다른 안내 문구를 가질 수 있도록 한다.
  if (status === 401) {
    return {
      classification: 'session-expired',
      message: selectMessage(status, code),
      code,
      status,
    }
  }

  // 3단계: code 필드가 있으면(F1) 도메인 오류로 판정한다.
  if (code !== undefined) {
    return {
      classification: 'domain',
      message: selectMessage(status, code),
      code,
      status,
    }
  }

  // 4단계: 그 외(F2 403 / F3 400 / 미지의 형태) — 상태 코드 기반. 원문 바디는
  // 절대 노출하지 않는다(REQ-ERR-006, INV-FE-006 — 예외를 던지지 않는다).
  return {
    classification: 'status-code',
    message: selectMessage(status, undefined),
    status,
  }
}

function selectMessage(status: number, code: string | undefined): string {
  if (code !== undefined && code in CODE_MESSAGES) {
    return CODE_MESSAGES[code]
  }
  return STATUS_MESSAGES[status] ?? FALLBACK_MESSAGE
}

/**
 * 바디에서 `code` 필드를 안전하게 추출한다. 바디가 객체가 아니거나(F2/F3의
 * 검증 실패 배열, 원문 텍스트, null 등) `code`가 문자열이 아니면 undefined를
 * 반환한다 — 어떤 입력에도 예외를 던지지 않는다(INV-FE-006, AC-FE-010).
 */
function extractErrorCode(body: unknown): string | undefined {
  if (typeof body !== 'object' || body === null || Array.isArray(body)) {
    return undefined
  }
  const candidate = (body as Record<string, unknown>).code
  return typeof candidate === 'string' && candidate.length > 0 ? candidate : undefined
}
