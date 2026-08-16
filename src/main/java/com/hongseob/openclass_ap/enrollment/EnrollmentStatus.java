package com.hongseob.openclass_ap.enrollment;

/**
 * 확정 수강신청 상태 (spec.md §A.4.2 — 2종 완전 열거).
 *
 * <p>{@code CANCELLED → ENROLLED} 역전이는 존재하지 않는다. 취소 후 같은
 * 강좌에 재신청하면 새 {@code ENROLLED} 행이 생성된다(REQ-WRK-007의 중복
 * 검사는 {@code ENROLLED} 행만 본다 — AC-ENR-040, M4 범위).</p>
 */
public enum EnrollmentStatus {
    ENROLLED,
    CANCELLED
}
