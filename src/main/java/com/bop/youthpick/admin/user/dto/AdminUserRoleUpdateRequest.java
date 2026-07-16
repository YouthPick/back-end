package com.bop.youthpick.admin.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminUserRoleUpdateRequest(
        @NotBlank(message = "role은 필수입니다.")
                @Pattern(regexp = "USER|ADMIN", message = "role은 USER 또는 ADMIN이어야 합니다.")
                String role) {}
