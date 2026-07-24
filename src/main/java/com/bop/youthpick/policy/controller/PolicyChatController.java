package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessagesResponse;
import com.bop.youthpick.policy.service.PolicyChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "정책 채팅")
@RestController
@RequestMapping("/api/v1/policies/{policyId}/chat/messages")
@RequiredArgsConstructor
@Validated
public class PolicyChatController {

    private final PolicyChatService policyChatService;

    @Operation(
            summary = "정책 채팅 메시지 조회",
            description = "특정 정책 채팅방의 메시지 목록을 afterId 기준으로 이후 메시지만 조회한다(회원 전용).")
    @GetMapping
    public ApiResponse<PolicyChatMessagesResponse> getMessages(
            @CurrentUser Long userId,
            @PathVariable Long policyId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "afterId는 0 이상이어야 합니다.")
                    Long afterId) {
        return ApiResponse.ok(policyChatService.getMessages(policyId, userId, afterId));
    }
}
