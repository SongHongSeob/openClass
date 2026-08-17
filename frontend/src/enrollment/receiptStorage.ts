// @MX:ANCHOR: [AUTO] 접수 시각의 유일한 영속 지점 — REQ-ENR-011. requestId를
// 키에 포함해 sessionStorage에 접수 시각(ms)을 기록·복원한다.
// tokenStorage.ts와 동일한 수명(탭 종료 시 소멸)·격리 범위(탭 단위)를 갖는
// storage를 재사용한다(design.md §A.4 — "저장 위치는 요청 식별자와 같은
// 수명을 갖는 곳이어야 한다").
// @MX:REASON: `/requests/:requestId`가 새로고침·주소 직접 진입의 일급
// 경로이므로(design.md §A.6), 접수 시각을 컴포넌트 상태에만 두면 마운트마다
// 재설정되어 REQ-ENR-006이 막으려던 혼잡 가중이 새로고침 반복만으로
// 재발한다(spec.md REQ-ENR-011 근거).

const KEY_PREFIX = 'openclass.enrollment.receiptAt.'

/**
 * `tokenStorage.ts`의 `TokenStorageLike`와 동일한 최소 구조적 인터페이스 —
 * 테스트에서 jsdom 없이 인메모리 목으로 대체할 수 있다. `removeItem`은 이
 * 모듈이 사용하지 않으므로(접수 시각은 종단 후에도 유지 — REQ-ENR-007) 포함하지
 * 않는다.
 */
export interface ReceiptStorageLike {
  getItem(key: string): string | null
  setItem(key: string, value: string): void
}

function keyFor(requestId: number): string {
  return `${KEY_PREFIX}${requestId}`
}

/** 접수(202) 직후 requestId와 함께 접수 시각을 기록한다(REQ-ENR-001·011). */
export function saveReceiptTimestamp(storage: ReceiptStorageLike, requestId: number, receivedAtMs: number): void {
  storage.setItem(keyFor(requestId), String(receivedAtMs))
}

/**
 * 보존된 접수 시각을 복원한다. 저장된 적 없거나 손상된 값(숫자가 아님)이면
 * `null`을 반환한다 — 예외를 던지지 않는다(INV-FE-006과 동일한 방어적 원칙).
 */
export function loadReceiptTimestamp(storage: ReceiptStorageLike, requestId: number): number | null {
  const raw = storage.getItem(keyFor(requestId))
  if (raw === null) {
    return null
  }
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : null
}
