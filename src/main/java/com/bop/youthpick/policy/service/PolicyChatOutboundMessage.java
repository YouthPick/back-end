package com.bop.youthpick.policy.service;

import com.bop.youthpick.policy.dto.PolicyChatMessageResponse;
import com.bop.youthpick.policy.entity.PolicyChatMessage;
import java.time.LocalDateTime;

record PolicyChatOutboundMessage(
        Long id,
        Long policyId,
        Long authorId,
        String authorName,
        String content,
        LocalDateTime createdAt,
        String clientMessageId) {

    static PolicyChatOutboundMessage from(PolicyChatMessage message, String clientMessageId) {
        Long authorId = message.getUser().getId();
        PolicyChatMessageResponse response = PolicyChatMessageResponse.from(message, authorId);
        return new PolicyChatOutboundMessage(
                response.id(),
                response.policyId(),
                authorId,
                response.authorName(),
                response.content(),
                response.createdAt(),
                clientMessageId);
    }

    PolicyChatMessageResponse forUser(Long userId) {
        return new PolicyChatMessageResponse(
                id,
                policyId,
                authorName,
                content,
                createdAt,
                authorId.equals(userId),
                authorId.equals(userId) ? clientMessageId : null);
    }
}
