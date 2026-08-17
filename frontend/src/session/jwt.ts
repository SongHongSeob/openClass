// @MX:ANCHOR: [AUTO] JWT 페이로드 디코드 — 서명 검증 없이 표시 목적으로만
// 사용한다(REQ-SES-008, spec.md §A.7). 세션 수립·복원(sessionState.ts)이 이
// 모듈을 통해서만 페이로드를 읽는다 — 다른 지점에서 토큰을 직접 파싱하지
// 않는다.
// @MX:REASON: design.md §A.5 — "서명 검증 없음"이라는 성질이 여러 소비자에게
// 반복해서 전달되어야 하는 보안 경계다. 단일 지점에 모아 두지 않으면 이 성질을
// 잊은 소비자가 role을 인가 판단에 쓰는 실수를 반복할 수 있다.

/** 백엔드 `JwtTokenProvider.generateToken`이 발급하는 클레임(sub·role·iat·exp). */
export interface JwtPayload {
  /** sub 클레임 — 회원 이메일. */
  sub: string
  /** role 클레임 — `MEMBER` | `ADMIN` 문자열. **표시 목적 한정**이며 인가
   * 판단의 근거로 사용해서는 안 된다(REQ-SES-008). */
  role: string
  /** exp 클레임 — 초 단위 Unix epoch. */
  exp: number
  /** iat 클레임 — 초 단위 Unix epoch. 표시에 사용하지 않으므로 옵셔널로 둔다. */
  iat?: number
}

/**
 * JWT의 페이로드(두 번째 세그먼트)를 base64url 디코드하여 반환한다.
 *
 * **서명을 검증하지 않는다.** 위조된 `role` 클레임으로 화면 표시를 속일 수
 * 있으나, 실제 API 호출은 백엔드의 서명 검증에서 401/403으로 차단되므로
 * 위험이 표시 목적에서만 국한된다(spec.md §A.7, REQ-SES-008).
 *
 * 손상된·형식 위반 토큰에 대해 예외를 던지지 않고 `null`을 반환한다
 * (INV-FE-006, AC-FE-033) — 호출자는 `null`을 "세션 폐기" 신호로 다룬다.
 */
export function decodeJwtPayload(token: string): JwtPayload | null {
  const parts = token.split('.')
  if (parts.length !== 3) {
    return null
  }

  try {
    const json = base64UrlDecode(parts[1])
    const parsed: unknown = JSON.parse(json)
    return isValidPayloadShape(parsed) ? parsed : null
  } catch {
    return null
  }
}

/**
 * exp(초 단위 Unix epoch)가 이미 지났는지 판정한다. `nowMs`를 주입 가능하게
 * 하여 테스트에서 시간을 고정할 수 있다(기본값은 실행 시각) — AC-FE-032.
 */
export function isExpired(exp: number, nowMs: number = Date.now()): boolean {
  return exp * 1000 <= nowMs
}

function isValidPayloadShape(value: unknown): value is JwtPayload {
  if (typeof value !== 'object' || value === null) {
    return false
  }
  const candidate = value as Record<string, unknown>
  return typeof candidate.sub === 'string' && typeof candidate.role === 'string' && typeof candidate.exp === 'number'
}

function base64UrlDecode(segment: string): string {
  const base64 = segment.replace(/-/g, '+').replace(/_/g, '/')
  const paddingLength = (4 - (base64.length % 4)) % 4
  const padded = base64 + '='.repeat(paddingLength)
  const binary = atob(padded)
  const bytes = Uint8Array.from(binary, (char) => char.charCodeAt(0))
  return new TextDecoder('utf-8').decode(bytes)
}
