package com.hongseob.openclass_ap.course;

/**
 * 강좌 모집 상태 (spec.md §A.3).
 *
 * <p>DB에도 {@code CHECK (status IN ('OPEN','CLOSED'))} 제약을 걸어 애플리케이션
 * 계층을 우회한 저장까지 방어한다 (plan.md §C.1.1 / REQ-CRS-005).</p>
 */
public enum CourseStatus {
    OPEN,
    CLOSED
}
