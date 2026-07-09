package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

public record RegisterManagementRequest(
        @NotNull(message = "사용자 ID는 필수입니다.") Long userId,
        @NotNull(message = "정책 ID는 필수입니다.") Long policyId,
        @NotBlank(message = "상태는 필수입니다.")
                @Pattern(regexp = "INTERESTED|APPLIED|COMPLETED", message = "유효하지 않은 상태값입니다.")
                String status,
        String memo,
        LocalDateTime endAt) {}
