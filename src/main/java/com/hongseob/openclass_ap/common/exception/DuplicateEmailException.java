package com.hongseob.openclass_ap.common.exception;

/**
 * 이미 등록된 이메일로 회원가입을 시도했을 때 발생한다 (REQ-SIGNUP-003, REQ-SIGNUP-006).
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("이미 등록된 이메일입니다: " + email);
    }
}
