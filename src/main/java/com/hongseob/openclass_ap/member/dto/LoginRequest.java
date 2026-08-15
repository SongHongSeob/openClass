package com.hongseob.openclass_ap.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 로그인 요청 바디.
 *
 * <p>의도적으로 {@code @Email} 형식 검증을 두지 않는다 — 형식이 잘못된 이메일과
 * 미가입 이메일을 서로 다른 응답 코드(400 vs 401)로 구분하면 계정 열거 공격의
 * 또 다른 신호가 되므로, 두 경우 모두 로그인 실패(401)로 동일하게 처리되도록
 * 검증을 최소화한다 (REQ-LOGIN-002).</p>
 */
public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
