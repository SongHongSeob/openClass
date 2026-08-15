package com.hongseob.openclass_ap.common.exception;

/**
 * 로그인 실패 시 던져진다 (REQ-LOGIN-002). 이메일 미가입·비밀번호 불일치를 구분하지
 * 않고 항상 동일한 예외·동일한 메시지를 사용한다 — 계정 열거(enumeration) 공격을
 * 막기 위해 두 실패 사유의 응답이 바이트 단위로 동일해야 한다(AC-AUTH-008).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("이메일 또는 비밀번호가 올바르지 않습니다");
    }
}
