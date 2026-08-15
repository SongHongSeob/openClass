package com.hongseob.openclass_ap.common.exception;

import com.hongseob.openclass_ap.common.response.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 도메인 예외를 HTTP 응답으로 변환하는 공통 핸들러.
 * 입력 검증 실패(400)는 Spring의 기본 @Valid 처리에 위임한다 — 별도 핸들러가
 * 필요한 것은 도메인 규칙 위반(예: 중복 이메일 → 409)뿐이다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(DuplicateEmailException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("DUPLICATE_EMAIL", ex.getMessage()));
    }
}
