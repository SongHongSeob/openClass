package com.hongseob.openclass_ap.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 종료 일시가 시작 일시보다 늦어야 함을 검증하는 클래스 레벨 제약
 * (REQ-NFR-001, AC-NFR-001).
 *
 * <p>필드 레벨 애노테이션(예: {@code @NotNull})만으로는 표현할 수 없는 교차
 * 필드 검증이므로 클래스 레벨 커스텀 제약으로 구현한다(plan.md §D "커스텀
 * 검증기 또는 compact-constructor 검증"). {@code @Valid}가 이미 컨트롤러
 * 파라미터에 붙어 있으므로({@code CourseAdminController}) 이 애노테이션을
 * 요청 DTO에 붙이는 것만으로 자동 적용된다.</p>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = DateRangeValidator.class)
public @interface ValidDateRange {

    String message() default "종료 일시는 시작 일시보다 이후여야 합니다";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
