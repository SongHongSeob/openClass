package com.hongseob.openclass_ap.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * {@link ValidDateRange}의 검증 로직.
 *
 * <p>{@code startsAt}·{@code endsAt} 중 하나라도 {@code null}이면 통과시킨다
 * — null 여부는 개별 필드의 {@code @NotNull}이 이미 담당하므로, 교차 필드
 * 제약은 필수값 검증을 중복하지 않는다는 Bean Validation 관례를 따른다.</p>
 */
public class DateRangeValidator implements ConstraintValidator<ValidDateRange, HasDateRange> {

    @Override
    public boolean isValid(HasDateRange value, ConstraintValidatorContext context) {
        if (value == null || value.startsAt() == null || value.endsAt() == null) {
            return true;
        }
        return value.endsAt().isAfter(value.startsAt());
    }
}
