package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PolicyComparisonCreateRequest(
        @NotNull(message = "비교할 정책 목록은 필수입니다.")
                @Size(min = 2, max = 3, message = "정책 비교는 2개에서 3개까지 가능합니다.")
                List<@NotNull(message = "정책 ID는 필수입니다.") Long> policyIds) {}
