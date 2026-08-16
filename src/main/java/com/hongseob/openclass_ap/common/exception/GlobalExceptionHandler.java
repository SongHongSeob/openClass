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

    /**
     * 로그인 실패는 사유(미가입/비밀번호 불일치)와 무관하게 항상 동일한 401 바디를
     * 반환한다 — 고정된 code/message만 사용하므로 응답이 바이트 단위로 동일하다
     * (AC-AUTH-008).
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of("INVALID_CREDENTIALS", ex.getMessage()));
    }

    /**
     * 존재하지 않는 강좌 식별자로 조회·변경을 요청했을 때 404를 반환한다
     * (plan.md §C.4.2 — 새 {@code @RestControllerAdvice}를 만들지 않고 기존 핸들러를
     * 확장한다. REQ-CAT-004 / REQ-ADM-009).
     */
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of("COURSE_NOT_FOUND", ex.getMessage()));
    }

    /**
     * 정원을 확정 인원 미만으로 축소하는 요청을 409 단일 코드로 거부한다
     * (plan.md §C.3 — 400은 정원 1 미만 같은 입력 형식 오류에만 쓴다. REQ-ADM-005).
     */
    @ExceptionHandler(CapacityBelowEnrollmentException.class)
    public ResponseEntity<ErrorResponse> handleCapacityBelowEnrollment(CapacityBelowEnrollmentException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of("CAPACITY_BELOW_ENROLLMENT", ex.getMessage()));
    }
}
