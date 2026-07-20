package com.bop.youthpick.admin.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminPolicyApplicationStatusUpdateRequest(
        @NotBlank(message = "status는 필수입니다.")
                @Pattern(
                        regexp = "INTERESTED|APPLIED|COMPLETED",
                        message = "status는 INTERESTED, APPLIED, COMPLETED 중 하나여야 합니다.")
                String status) {}
