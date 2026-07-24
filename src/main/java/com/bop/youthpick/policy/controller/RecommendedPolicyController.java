package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.RecommendedPolicyResponse;
import com.bop.youthpick.policy.service.RecommendedPolicyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천 정책")
@RestController
@RequestMapping("/api/v1/recommended-policies")
@RequiredArgsConstructor
public class RecommendedPolicyController {

    private final RecommendedPolicyService recommendedPolicyService;

    /** 맞춤정책 조회 (회원 전용). 로그인 사용자의 온보딩 프로필과 정책 자격조건을 매칭해 점수순으로 내린다. */
    @Operation(
            summary = "맞춤 정책 추천 조회",
            description = "맞춤정책 조회(회원 전용). 로그인 사용자의 온보딩 프로필과 정책 자격조건을 매칭해 점수순으로 내린다.")
    @GetMapping
    public ApiResponse<List<RecommendedPolicyResponse>> getRecommendations(
            @CurrentUser Long userId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region) {
        return ApiResponse.ok(
                recommendedPolicyService.getRecommendations(userId, category, keyword, region));
    }
}
