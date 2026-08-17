package com.hongseob.openclass_ap.enrollment.request;

/**
 * 큐 행의 처리 상태 (spec.md §A.4.1 — 2종 완전 열거).
 *
 * <p>{@code PROCESSING} 중간 상태는 의도적으로 두지 않는다 — 워커가 요청 1건을
 * 1개 트랜잭션으로 처리하므로 처리 중인 행은 그 트랜잭션의 행 잠금으로 표시되고,
 * 애플리케이션이 처리 도중 죽으면 트랜잭션이 롤백되어 행이 자동으로
 * {@code PENDING}으로 돌아간다. 멈춘 {@code PROCESSING} 행이라는 상태 자체가
 * 존재하지 않으므로 복구 스위퍼도 필요 없다(spec.md §A.4.1, plan.md §G
 * 안티패턴).</p>
 */
public enum RequestState {
    PENDING,
    DONE
}
