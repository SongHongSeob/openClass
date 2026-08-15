package com.hongseob.openclass_ap.member.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 회원가입 요청 바디.
 *
 * <p>의도적으로 role 필드를 두지 않는다 — 요청 본문에 role이 포함되어 있어도
 * Jackson이 알 수 없는 필드로 무시하며(REQ-SIGNUP-005), 애초에 이 레코드가
 * role을 받을 방법이 없으므로 서비스 계층까지 전달될 경로가 존재하지 않는다.</p>
 */
public record SignupRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        String name
) {

    /**
     * 앞뒤 공백만 미리 제거한다 — 그렇지 않으면 "  a@example.com  "처럼 정상적인
     * 이메일도 공백 때문에 @Email 형식 검증에서 걸린다(AC-AUTH-006). 소문자 변환을
     * 포함한 완전한 정규화는 여전히 Member.normalizeEmail() 한 곳에서만 수행한다
     * (plan.md §C.1) — 여기서는 검증 타이밍 문제를 해소하기 위한 트림만 한다.
     */
    public SignupRequest {
        email = email == null ? null : email.trim();
    }
}
