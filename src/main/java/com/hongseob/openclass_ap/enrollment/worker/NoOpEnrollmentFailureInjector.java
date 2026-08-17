package com.hongseob.openclass_ap.enrollment.worker;

import org.springframework.stereotype.Component;

/**
 * {@link EnrollmentFailureInjector}의 프로덕션 기본 구현 — 아무 동작도 하지
 * 않는다. AC-ENR-016/017을 재현하는 테스트만 {@code @Primary} 테스트 전용
 * 구현으로 이 빈을 대체한다.
 */
@Component
public class NoOpEnrollmentFailureInjector implements EnrollmentFailureInjector {

    @Override
    public void afterEnrollmentPersisted(Long requestId) {
        // 프로덕션 기본 동작 — 아무 것도 하지 않는다.
    }
}
