package com.hongseob.openclass_ap.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 관리자 강좌 생성 요청 바디 (plan.md §C.4.1-1 — DTO는 전부 record).
 *
 * <p>정원 1 미만은 {@code @Min(1)}이 Spring의 기본 {@code @Valid} 처리로 400을
 * 반환한다(REQ-ADM-004, AC-ADM-003) — 이는 값 자체가 유효하지 않은 입력 형식
 * 오류이므로 정원 축소 거부(409)와 다른 계층에서 처리된다(plan.md §C.3).</p>
 */
public record CourseCreateRequest(
        @NotBlank String title,
        String description,
        @NotNull @Min(1) Integer capacity,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt
) {
}
