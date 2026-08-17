package com.hongseob.openclass_ap.enrollment.request;

/**
 * 큐 행이 나타내는 요청 종류 (spec.md §A.4.1 — 3종 완전 열거).
 *
 * <p>DB에도 {@code CHECK (request_type IN (...))} 제약을 걸어 애플리케이션
 * 계층을 우회한 저장까지 방어한다(REQ-QUE-007, AC-ENR-004).</p>
 */
public enum RequestType {
    ENROLL,
    CANCEL,
    CAPACITY_INCREASE
}
