package com.bop.youthpick.policy.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyComparisonItemResponse;
import com.bop.youthpick.policy.service.PolicyComparisonService;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 정책 비교 (비회원 허용). 비교 결과는 저장하지 않고 요청받은 정책들을 그때그때 조회해 내려주는 순수 조회다 — 그래서 생성(POST) + 식별자 재조회가 아니라 쿼리
 * 파라미터를 받는 단일 GET이다. 공유가 필요하면 이 요청 URL 자체가 공유 링크가 된다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/policy-comparisons")
@RequiredArgsConstructor
public class PolicyComparisonController {

    private final PolicyComparisonService policyComparisonService;

    @GetMapping
    public ApiResponse<List<PolicyComparisonItemResponse>> compare(
            @RequestParam
                    @NotEmpty(message = "비교할 정책 목록은 필수입니다.")
                    @Size(min = 2, max = 3, message = "정책 비교는 2개에서 3개까지 가능합니다.")
                    List<Long> policyIds) {
        return ApiResponse.ok(policyComparisonService.compare(policyIds));
    }
}
