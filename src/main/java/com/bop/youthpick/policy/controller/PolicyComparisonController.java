package com.bop.youthpick.policy.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyComparisonCreateRequest;
import com.bop.youthpick.policy.dto.PolicyComparisonResponse;
import com.bop.youthpick.policy.service.PolicyComparisonService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 정책 비교 (비회원 허용). DB에 저장하지 않는 stateless 설계 — {@link PolicyComparisonService} 클래스 주석 참고. */
@RestController
@RequestMapping("/api/v1/policy-comparisons")
@RequiredArgsConstructor
public class PolicyComparisonController {

    private final PolicyComparisonService policyComparisonService;

    @PostMapping
    public ResponseEntity<ApiResponse<PolicyComparisonResponse>> create(
            @Valid @RequestBody PolicyComparisonCreateRequest request) {
        PolicyComparisonResponse response = policyComparisonService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{comparisonId}")
    public ApiResponse<PolicyComparisonResponse> find(@PathVariable String comparisonId) {
        return ApiResponse.ok(policyComparisonService.find(comparisonId));
    }
}
