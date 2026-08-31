package com.hongseob.openclass_ap.member.dto;

import com.hongseob.openclass_ap.member.Member;
import java.time.LocalDateTime;

/**
 * 관리자 회원 관리 화면 응답 바디. {@code passwordHash}는 절대 포함하지 않는다.
 */
public record MemberResponse(Long id, String email, String name, String role, LocalDateTime createdAt) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(
                member.getId(),
                member.getEmail(),
                member.getName(),
                member.getRole().name(),
                member.getCreatedAt()
        );
    }
}
