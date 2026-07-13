package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record RegisterPolicyApplicationRequest(
        @NotNull(message = "사용자 ID는 필수입니다.") Long userId,
        @NotNull(message = "정책 ID는 필수입니다.") Long policyId,
        @NotBlank(message = "상태는 필수입니다.")
                @Pattern(regexp = "INTERESTED|APPLIED|COMPLETED", message = "유효하지 않은 상태값입니다.")
                String status,
        @Size(max = 500, message = "메모는 500자를 초과할 수 없습니다.") String memo,
        LocalDateTime endAt) {}
