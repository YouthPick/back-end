package com.bop.youthpick.auth.dto;

import com.bop.youthpick.user.entity.User;

public record AuthUserResponse(Long id, String email, String nickname, String role) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(), user.getEmail(), user.getNickname(), user.getRole().name());
    }
}
