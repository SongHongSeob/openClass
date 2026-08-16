package com.hongseob.openclass_ap.enrollment.request;

/**
 * 큐 행의 처리 결과 (spec.md §A.4.1 — {@code request_type}별 가능 값의 합집합을
 * 하나의 열로 완전 열거). {@code state='PENDING'}이면 이 값은 {@code NULL}이고,
 * {@code state='DONE'}이면 정확히 하나가 설정된다(INV-ENR-006).
 *
 * <p>M1은 {@code ENROLL}의 {@link #SUCCESS}/{@link #WAITLISTED} 두 값만 실제로
 * 도달 가능한 프로덕션 경로를 갖는다({@link com.hongseob.openclass_ap.enrollment.worker.EnrollmentRequestProcessor}
 * 참고). 나머지 값은 M2(CLOSED/REJECTED/FAILED) · M4(CANCELLED) ·
 * M5(PROMOTED/NOOP)가 각각 도달 경로를 추가하며, 도메인 열거 자체는 스키마를
 * 다시 바꾸지 않기 위해 M1에서 전부 확정한다(plan.md §C.1).</p>
 */
public enum RequestResult {
    SUCCESS,
    WAITLISTED,
    CLOSED,
    REJECTED,
    FAILED,
    CANCELLED,
    PROMOTED,
    NOOP
}
