package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PolicyApplicationCreateChecklistRequest(
        @NotNull(message = "신청관리 ID는 필수입니다.") Long applicationId,
        @NotBlank(message = "체크리스트 내용은 필수입니다.")
                @Size(max = 500, message = "체크리스트 내용은 500자 이하여야 합니다.")
                String message) {}
