package com.bop.youthpick.user.dto;

import com.bop.youthpick.user.entity.LoginHistory;
import java.time.LocalDateTime;

public record LoginHistoryResponse(
        Long id, Long userId, LocalDateTime createdAt, LocalDateTime updatedAt) {
    public static LoginHistoryResponse from(LoginHistory history) {
        return new LoginHistoryResponse(
                history.getId(),
                history.getUser().getId(),
                history.getCreatedAt(),
                history.getUpdatedAt());
    }
}
