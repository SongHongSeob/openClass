package com.hongseob.openclass_ap.enrollment.dto;

/**
 * 접수 응답 바디 — 큐 행의 순서값(요청 식별자)만 반환한다(REQ-QUE-002).
 * 확정 여부는 이 응답에 포함되지 않는다 — 접수는 큐 적재만 수행하기 때문이다.
 */
public record EnrollmentReceiptResponse(Long requestId) {
}
