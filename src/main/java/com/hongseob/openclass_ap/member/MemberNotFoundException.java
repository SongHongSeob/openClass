package com.hongseob.openclass_ap.member;

/**
 * 존재하지 않는 회원 식별자로 조회·변경을 시도했을 때 발생한다.
 */
public class MemberNotFoundException extends RuntimeException {

    public MemberNotFoundException(Long id) {
        super("존재하지 않는 회원입니다: " + id);
    }
}
