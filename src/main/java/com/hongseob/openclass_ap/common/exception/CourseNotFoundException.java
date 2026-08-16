package com.hongseob.openclass_ap.common.exception;

/**
 * 존재하지 않는 강좌 식별자로 조회·변경을 시도했을 때 발생한다 (REQ-CAT-004, REQ-ADM-009).
 */
public class CourseNotFoundException extends RuntimeException {

    public CourseNotFoundException(Long id) {
        super("존재하지 않는 강좌입니다: " + id);
    }
}
