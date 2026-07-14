package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminPolicyApplicationResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long id,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long userId,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long policyId,
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
