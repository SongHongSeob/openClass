package com.hongseob.openclass_ap.member.dto;

/**
 * 로그인 응답 바디 — 액세스 토큰만 반환한다.
 */
public record LoginResponse(String accessToken) {
}
