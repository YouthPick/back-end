package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessagesResponse;
import com.bop.youthpick.policy.service.PolicyChatService;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies/{policyId}/chat/messages")
@RequiredArgsConstructor
@Validated
public class PolicyChatController {

    private final PolicyChatService policyChatService;

    @GetMapping
    public ApiResponse<PolicyChatMessagesResponse> getMessages(
            @CurrentUser Long userId,
            @PathVariable Long policyId,
            @RequestParam(defaultValue = "0") @Min(value = 0, message = "afterId는 0 이상이어야 합니다.")
                    Long afterId) {
        return ApiResponse.ok(policyChatService.getMessages(policyId, userId, afterId));
    }
}
