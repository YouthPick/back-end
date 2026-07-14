package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public record ApplicationChecklistItemResponse(
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long id,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Long policyApplicationId,
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
