package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyRecentViewResponse;
import com.bop.youthpick.policy.service.PolicyRecentViewService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recent-policies")
@RequiredArgsConstructor
public class PolicyRecentViewController {

    private final PolicyRecentViewService policyRecentViewService;

    /** 최근 본 정책 목록 (회원 전용). 마지막 조회 시각 내림차순. */
    @GetMapping
    public ApiResponse<List<PolicyRecentViewResponse>> list(
            @CurrentUser Long userId, @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyRecentViewResponse> page =
                policyRecentViewService.getRecentPolicies(userId, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }
}
