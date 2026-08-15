package com.hongseob.openclass_ap.member.dto;

import com.hongseob.openclass_ap.member.Member;

/**
 * 회원가입 응답 바디.
 */
public record SignupResponse(Long id, String email, String role) {

    public static SignupResponse from(Member member) {
        return new SignupResponse(member.getId(), member.getEmail(), member.getRole().name());
    }
}
