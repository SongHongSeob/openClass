package com.hongseob.openclass_ap.common.exception;

/**
 * 정원을 확정 인원 미만으로 축소하려는 요청에서 발생한다(REQ-ADM-005).
 * 정확히 확정 인원과 같은 값으로의 축소는 허용이므로 이 예외는 그 경계값에서는
 * 발생하지 않는다(plan.md §C.3, AC-ADM-004).
 */
public class CapacityBelowEnrollmentException extends RuntimeException {

    public CapacityBelowEnrollmentException(Long courseId, Integer requestedCapacity, Integer enrolledCount) {
        super("정원(" + requestedCapacity + ")을 확정 인원(" + enrolledCount + ") 미만으로 줄일 수 없습니다: courseId=" + courseId);
    }
}
