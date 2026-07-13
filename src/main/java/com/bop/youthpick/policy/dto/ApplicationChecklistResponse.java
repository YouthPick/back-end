package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import java.time.LocalDateTime;

public record ApplicationChecklistResponse(
        Long id,
        Long policyApplicationId,
        String message,
        boolean checked,
        LocalDateTime createdAt) {

    public static ApplicationChecklistResponse from(ApplicationChecklist checklist) {
        return new ApplicationChecklistResponse(
                checklist.getId(),
                checklist.getApplication().getId(),
                checklist.getContent(),
                checklist.isChecked(),
                checklist.getCreatedAt());
    }
}
