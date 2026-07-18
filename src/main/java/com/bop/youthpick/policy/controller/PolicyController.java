package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyCardResponse;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.service.PolicyService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/policies")
@RequiredArgsConstructor
public class PolicyController {

    private final PolicyService policyService;

    /**
     * 정책 목록(카드) 조회 (비회원 허용). 최신순 서버 고정 정렬, category(표준 5분류)는 선택 필터, meta에
     * page/totalCount/totalPages.
     */
    @GetMapping
    public ApiResponse<List<PolicyCardResponse>> getCards(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyCardResponse> page = policyService.getCards(category, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    /** 정책 상세 조회 (비회원 허용). 로그인 사용자의 조회는 최근 본 정책으로 기록된다. */
    @GetMapping("/{policyId}")
    public ApiResponse<PolicyDetailResponse> getDetail(
            @PathVariable Long policyId, @CurrentUser(required = false) Long userId) {
        return ApiResponse.ok(policyService.getDetail(policyId, userId));
    }
}
