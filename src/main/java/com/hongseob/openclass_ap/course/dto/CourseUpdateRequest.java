package com.hongseob.openclass_ap.course.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 관리자 강좌 수정 요청 바디 (plan.md §C.4.1-1 — DTO는 전부 record).
 *
 * <p>{@code capacity}가 확정 인원 미만인지는 이 레코드가 아니라 서비스 계층이
 * 검증한다(409, plan.md §C.3) — 여기서는 정원 1 미만(400)만 형식 검증으로
 * 걸러낸다(REQ-ADM-004).</p>
 */
public record CourseUpdateRequest(
        @NotBlank String title,
        String description,
        @NotNull @Min(1) Integer capacity,
        @NotNull LocalDateTime startsAt,
        @NotNull LocalDateTime endsAt
) {
}
