package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record AdminPolicyUpdateRequest(
        @NotBlank(message = "정책명은 필수입니다.") @Size(max = 300, message = "정책명은 300자 이하여야 합니다.")
                String policyName,
        @Size(max = 255, message = "주관기관명은 255자 이하여야 합니다.") String organizationName,
        @Size(max = 2000, message = "설명은 2000자 이하여야 합니다.") String description,
        @NotBlank(message = "대분류는 필수입니다.") @Size(max = 64, message = "대분류는 64자 이하여야 합니다.")
                String largeCategory,
        @Size(max = 64, message = "중분류는 64자 이하여야 합니다.") String middleCategory,
        @NotNull(message = "신청 시작일은 필수입니다.") LocalDate applicationStartDate,
        @NotNull(message = "신청 마감일은 필수입니다.") LocalDate applicationEndDate,
        @Size(max = 500, message = "신청 URL은 500자 이하여야 합니다.") String applicationUrl,
        @NotNull(message = "지역 코드 목록은 필수입니다.") List<String> regionCodes) {}
