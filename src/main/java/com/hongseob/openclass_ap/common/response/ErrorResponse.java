package com.hongseob.openclass_ap.common.response;

/**
 * 공통 에러 응답 바디.
 */
public record ErrorResponse(String code, String message) {

    public static ErrorResponse of(String code, String message) {
        return new ErrorResponse(code, message);
    }
}
