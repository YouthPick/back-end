package com.bop.youthpick.policy.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PolicyChatMessageCreateRequest(
        @NotBlank(message = "메시지 내용은 필수입니다.") @Size(max = 1000, message = "메시지 내용은 1000자 이하여야 합니다.")
                String content,
        @Size(max = 100, message = "클라이언트 메시지 ID는 100자 이하여야 합니다.") String clientMessageId) {}
