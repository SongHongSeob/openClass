package com.hongseob.openclass_ap.common.exception;

/**
 * 존재하지 않거나 본인 소유가 아닌 신청 요청을 조회했을 때 발생한다 (REQ-STS-002,
 * AC-ENR-025). 두 경우를 동일한 예외·동일한 404 응답으로 처리해 요청의 존재
 * 여부 자체를 노출하지 않는다 — 소유자가 아닌 회원에게는 "존재하지 않음"과
 * "존재하지만 접근 불가"를 구분해 줄 이유가 없다(IDOR 방지, 감사 D12와 동일한
 * 계보의 소유권 검증).
 */
public class EnrollmentRequestNotFoundException extends RuntimeException {

    public EnrollmentRequestNotFoundException(Long id) {
        super("존재하지 않거나 접근 권한이 없는 신청 요청입니다: " + id);
    }
}
