package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPolicyApplicationResponse(
        Long id,
        Long userId,
        Long policyId,
        String policyName,
        ApplicationStatus status,
        String memo,
        LocalDate deadline,
        LocalDateTime createdAt) {
    public static AdminPolicyApplicationResponse from(PolicyApplication application) {
        return new AdminPolicyApplicationResponse(
                application.getId(),
                application.getUser().getId(),
                application.getPolicy().getId(),
                application.getPolicy().getTitle(),
                application.getStatus(),
                application.getMemo(),
                application.getEndAt() == null ? null : application.getEndAt().toLocalDate(),
                application.getCreatedAt());
    }
}
