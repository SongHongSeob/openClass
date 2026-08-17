package com.hongseob.openclass_ap.common.exception;

/**
 * 강좌 식별자가 형식적으로 유효하지 않을 때(0 이하) 발생한다(M6, REQ-NFR-003,
 * AC-ENR-046). 존재하지 않는 강좌({@link CourseNotFoundException}, 404)와는
 * 구분된다 — 이 예외는 DB 조회 이전에 형식 검증 단계에서 던져지며 400으로
 * 응답한다. 숫자가 아닌 값은 Spring의 기본 타입 변환 실패 처리(400)가 이미
 * 담당하므로 이 예외의 대상이 아니다.
 */
public class InvalidCourseIdException extends RuntimeException {

    public InvalidCourseIdException(Long id) {
        super("유효하지 않은 강좌 식별자입니다: " + id);
    }
}
