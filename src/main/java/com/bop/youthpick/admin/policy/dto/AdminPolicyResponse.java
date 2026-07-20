package com.bop.youthpick.admin.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminPolicyResponse(
        Long id,
        String policyNo,
        String policyName,
        String organizationName,
        String description,
        String largeCategory,
        String middleCategory,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String applicationUrl,
        int viewCount,
        PolicyVisibility visibilityStatus,
        List<String> regionCodes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime deletedAt) {
    public static AdminPolicyResponse from(Policy policy, List<String> regionCodes) {
        return new AdminPolicyResponse(
                policy.getId(),
                policy.getPolicyNo(),
                policy.getTitle(),
                policy.getOrganizationName(),
                policy.getDescription(),
                policy.getCategory(),
                policy.getMiddleCategory(),
                policy.getApplicationStartDate(),
                policy.getApplicationEndDate(),
                policy.getApplicationUrl(),
                policy.getViewCount(),
                policy.getVisibility(),
                regionCodes,
                policy.getCreatedAt(),
                policy.getUpdatedAt(),
                policy.getDeletedAt());
    }
}
