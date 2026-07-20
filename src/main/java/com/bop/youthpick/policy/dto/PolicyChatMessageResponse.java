package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.PolicyChatMessage;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;

public record PolicyChatMessageResponse(
        Long id,
        Long policyId,
        String authorName,
        String content,
        LocalDateTime createdAt,
        boolean mine,
        @JsonInclude(JsonInclude.Include.NON_NULL) String clientMessageId) {

    private static final String DEFAULT_AUTHOR_NAME = "사용자";

    public static PolicyChatMessageResponse from(PolicyChatMessage message, Long currentUserId) {
        String nickname = message.getUser().getNickname();
        String authorName = nickname == null || nickname.isBlank() ? DEFAULT_AUTHOR_NAME : nickname;
        return new PolicyChatMessageResponse(
                message.getId(),
                message.getPolicy().getId(),
                authorName,
                message.getContent(),
                message.getCreatedAt(),
                message.getUser().getId().equals(currentUserId),
                null);
    }
}
