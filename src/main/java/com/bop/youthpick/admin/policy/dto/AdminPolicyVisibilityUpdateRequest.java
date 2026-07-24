package com.bop.youthpick.admin.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminPolicyVisibilityUpdateRequest(
        @NotBlank(message = "visibilityStatus는 필수입니다.")
                @Pattern(
                        regexp = "VISIBLE|HIDDEN",
                        message = "visibilityStatus는 VISIBLE 또는 HIDDEN이어야 합니다.")
                String visibilityStatus) {}
