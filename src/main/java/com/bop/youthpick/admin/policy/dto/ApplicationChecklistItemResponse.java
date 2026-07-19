package com.bop.youthpick.admin.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import java.time.LocalDateTime;

public record ApplicationChecklistItemResponse(
        Long id,
        Long policyApplicationId,
        boolean checked,
        String description,
        LocalDateTime createdAt) {
    public static ApplicationChecklistItemResponse from(ApplicationChecklist item) {
        return new ApplicationChecklistItemResponse(
                item.getId(),
                item.getApplication().getId(),
                item.isChecked(),
                item.getContent(),
                item.getCreatedAt());
    }
}
