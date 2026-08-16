package com.hongseob.openclass_ap.waitlist;

/**
 * 대기명단 항목 상태 (spec.md §A.4.3 — 4종 완전 열거).
 *
 * <p>"활성(active)"은 이 SPEC 전체에서 {@link #WAITING}인 항목만을 가리킨다.
 * 종단 3종({@link #PROMOTED}·{@link #CANCELLED}·{@link #DUPLICATE})에서 나가는
 * 전이는 존재하지 않는다. {@link #DUPLICATE}는 승격 시점에 이미 유효한 확정을
 * 보유해 승격 부적격으로 판정되어 건너뛰어진 항목을 가리킨다(REQ-WL-009,
 * M4 범위).</p>
 */
public enum WaitlistStatus {
    WAITING,
    PROMOTED,
    CANCELLED,
    DUPLICATE
}
