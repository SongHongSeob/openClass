package com.hongseob.openclass_ap.common.exception;

/**
 * 존재하지 않거나 본인 소유가 아니거나 이미 종단 상태(승격·취소·중복 종결)인
 * 대기명단 항목을 취소 요청했을 때 발생한다(M4, REQ-WL-008, AC-ENR-034).
 * 세 경우를 동일한 예외·동일한 404 응답으로 처리해 존재 여부 자체를
 * 노출하지 않는다 — {@link EnrollmentRequestNotFoundException}과 동일한
 * 계보의 소유권 검증(감사 D12, IDOR 방지).
 */
public class WaitlistEntryNotFoundException extends RuntimeException {

    public WaitlistEntryNotFoundException(Long id) {
        super("존재하지 않거나 접근 권한이 없는 대기명단 항목입니다: " + id);
    }
}
