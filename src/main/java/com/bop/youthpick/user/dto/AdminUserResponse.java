package com.bop.youthpick.user.dto;

import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.entity.User;
import java.time.LocalDateTime;

public record AdminUserResponse(
        Long id,
        String provider,
        String providerId,
        Role role,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getProvider(),
                user.getProviderId(),
                user.getRole(),
                user.getCreatedAt(),
                user.getDeletedAt());
    }
}
