package com.bop.youthpick.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record OnboardingProfileRequest(
        @NotNull(message = "출생연도는 필수입니다.")
        @Min(value = 1950, message = "출생연도가 올바르지 않습니다.")
        @Max(value = 2015, message = "출생연도가 올바르지 않습니다.")
        Integer birthYear,

        @NotBlank(message = "거주 지역은 필수입니다.")
        @Size(max = 10, message = "지역 코드 형식이 올바르지 않습니다.")
        String regionCode,

        @Size(max = 16, message = "취업상태 코드 형식이 올바르지 않습니다.")
        String employmentStatus,

        @Size(max = 16, message = "학력 코드 형식이 올바르지 않습니다.")
        String educationLevel,

        @Size(max = 500, message = "관심분야는 500자를 초과할 수 없습니다.")
        String categories,

        @Size(max = 700, message = "관심키워드는 700자를 초과할 수 없습니다.")
        String keywords
) {
}
