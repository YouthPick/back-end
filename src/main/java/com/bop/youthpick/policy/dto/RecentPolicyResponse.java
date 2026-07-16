package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.RecentPolicyView;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 최근 본 정책 목록의 카드 응답. */
public record RecentPolicyResponse(
        Long policyId,
        String title,
        String description,
        String category,
        String middleCategory,
        String organizationName,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        String applicationUrl,
        LocalDateTime viewedAt) {

    public static RecentPolicyResponse from(RecentPolicyView view) {
        Policy policy = view.getPolicy();
        return new RecentPolicyResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getDescription(),
                policy.getCategory(),
                policy.getMiddleCategory(),
                policy.getOrganizationName(),
                policy.getApplicationStartDate(),
                policy.getApplicationEndDate(),
                policy.getApplicationUrl(),
                view.getViewedAt());
    }
}
