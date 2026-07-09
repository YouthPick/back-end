package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PolicyApplicationResponse(
        Long id,
        Long policyId,
        String policyTitle,
        String policyCategory,
        LocalDate policyApplicationEndDate,
        ApplicationStatus status,
        String memo,
        LocalDateTime endAt,
        LocalDateTime createdAt) {

    public static PolicyApplicationResponse from(PolicyApplication application) {
        var policy = application.getPolicy();
        return new PolicyApplicationResponse(
                application.getId(),
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getApplicationEndDate(),
                application.getStatus(),
                application.getMemo(),
                application.getEndAt(),
                application.getCreatedAt());
    }
}
