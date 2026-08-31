package com.hongseob.openclass_ap.member;

/**
 * ADMIN이 자기 자신의 역할을 변경하려는 요청에서 발생한다.
 */
public class SelfRoleChangeNotAllowedException extends RuntimeException {

    public SelfRoleChangeNotAllowedException() {
        super("자기 자신의 역할은 변경할 수 없습니다.");
    }
}
