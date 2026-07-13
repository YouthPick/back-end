package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import java.time.LocalDateTime;

public record PolicyApplicationChecklistResponse(
        Long id,
        Long policyApplicationId,
        String message,
        boolean checked,
        LocalDateTime createdAt) {

    public static PolicyApplicationChecklistResponse from(PolicyApplicationChecklist checklist) {
        return new PolicyApplicationChecklistResponse(
                checklist.getId(),
                checklist.getApplication().getId(),
                checklist.getContent(),
                checklist.isChecked(),
                checklist.getCreatedAt());
    }
}
