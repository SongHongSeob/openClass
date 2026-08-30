// @MX:ANCHOR: [AUTO] 세션 토큰의 유일한 영속 지점 — REQ-SES-004(탭 수명 한정),
// plan.md §C.4(sessionStorage 채택). SessionContext만 이 모듈을 통해 토큰을
// 읽고 쓴다 — 다른 지점에서 storage를 직접 건드리지 않는다.
// @MX:REASON: 저장 위치(sessionStorage, localStorage 아님)가 spec.md
// REQ-SES-004의 규범 대상이다. 이 결정을 단일 지점에 모아 두지 않으면 후속
// 코드가 무심코 localStorage로 회귀시킬 수 있다(§C.4가 명시적으로 기각한
// 후보).

const STORAGE_KEY = 'openclass.session.token'

/**
 * DOM lib의 `Storage`와 구조적으로 호환되는 최소 인터페이스. 이 모듈의 함수는
 * 이 인터페이스만 요구하므로, 테스트에서 jsdom 없이 인메모리 목으로 대체할 수
 * 있다.
 */
export interface TokenStorageLike {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
  removeItem(key: string): void
}

/** 로그인 성공 시 토큰을 저장한다(REQ-SES-002). */
export function saveToken(storage: TokenStorageLike, token: string): void {
  storage.setItem(STORAGE_KEY, token)
}

/** 앱 최초 렌더 시 저장된 토큰을 복원한다(design.md §A.5 [복원]). */
export function loadToken(storage: TokenStorageLike): string | null {
  return storage.getItem(STORAGE_KEY)
}

/** 로그아웃 또는 401 감지 시 토큰을 제거한다(REQ-SES-005, REQ-SES-007). */
export function clearToken(storage: TokenStorageLike): void {
  storage.removeItem(STORAGE_KEY)
}
