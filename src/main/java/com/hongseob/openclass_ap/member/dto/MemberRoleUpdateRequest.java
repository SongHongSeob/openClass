package com.hongseob.openclass_ap.member.dto;

import com.hongseob.openclass_ap.member.MemberRole;
import jakarta.validation.constraints.NotNull;

/**
 * 관리자 회원 역할 변경 요청 바디.
 */
public record MemberRoleUpdateRequest(@NotNull MemberRole role) {
}
