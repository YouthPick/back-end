package com.bop.youthpick.policy.dto;

import java.util.List;

public record PolicyChatMessagesResponse(
        List<PolicyChatMessageResponse> messages, Long nextCursor) {

    public static PolicyChatMessagesResponse of(
            List<PolicyChatMessageResponse> messages, Long originalCursor) {
        Long nextCursor =
                messages.isEmpty() ? originalCursor : messages.get(messages.size() - 1).id();
        return new PolicyChatMessagesResponse(messages, nextCursor);
    }
}
