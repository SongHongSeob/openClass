// @MX:NOTE: [AUTO] 종단 결과 8종 + 미지 값 대체 문구의 단일 지점 — REQ-ENR-009.
// pollingSchedule.ts의 `isTerminalStatus`가 "종단인가"를 판정하고, 이 모듈은
// "종단이면 무엇을 보여줄 것인가"만 담당한다 — 두 책임을 분리해 판정 로직이
// 문구 테이블에 오염되지 않게 한다.

/** research.md·spec.md §A.4가 열거한 현재 알려진 종단 값 8종의 안내 문구. */
const TERMINAL_MESSAGES: Record<string, string> = {
  SUCCESS: '수강신청이 확정되었습니다.',
  WAITLISTED: '정원이 가득 차 대기명단에 등록되었습니다.',
  CLOSED: '강좌 모집이 마감되어 신청이 처리되지 않았습니다.',
  REJECTED: '요청이 거부되었습니다. 중복 신청 여부를 확인해 주세요.',
  FAILED: '처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.',
  CANCELLED: '취소가 완료되었습니다.',
  PROMOTED: '정원 증설로 대기명단에서 승격되어 확정되었습니다.',
  NOOP: '승격 대상이 없어 별도로 처리되지 않았습니다.',
}

/**
 * 목록에 없는 미지 상태값(REQ-ENR-009 — 백엔드가 결과값을 추가한 경우)에
 * 대한 일반 안내 문구. 화면은 이 값을 오류로 취급하지 않고 그대로 표시한다.
 */
const FALLBACK_MESSAGE = '요청이 처리되었습니다. 최신 상태는 새로고침으로 다시 확인해 주세요.'

/** 종단 상태값 → 사용자 안내 문구. 미지 값은 {@link FALLBACK_MESSAGE}로 대체된다. */
export function selectTerminalMessage(status: string): string {
  return TERMINAL_MESSAGES[status] ?? FALLBACK_MESSAGE
}
