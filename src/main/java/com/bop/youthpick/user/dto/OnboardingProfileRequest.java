package com.bop.youthpick.user.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * categories/keywords는 온보딩 화면에서 체크박스로 다중 선택되는 값이라
 * 콤마 문자열이 아니라 배열로 받는다. 콤마 결합(저장 포맷)은 서비스 계층 책임이다.
 * categories는 대분류 5개 중 최대 3개, keywords는 전체 19개 중 선택.
 */
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

        @Size(max = 3, message = "관심분야는 최대 3개까지 선택할 수 있습니다.")
        List<@NotBlank String> categories,

        @Size(max = 19, message = "관심키워드는 최대 19개까지 선택할 수 있습니다.")
        List<@NotBlank String> keywords
) {
}
