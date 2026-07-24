package com.bop.youthpick.admin.sync.controller;

import com.bop.youthpick.admin.sync.dto.PolicySyncJobSummaryResponse;
import com.bop.youthpick.admin.sync.service.AdminPolicySyncJobService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 정책 동기화 작업")
@RestController
@RequestMapping("/api/v1/admin/policy-sync-jobs")
@RequiredArgsConstructor
public class AdminPolicySyncJobController {

    private final AdminPolicySyncJobService adminPolicySyncJobService;

    @Operation(summary = "정책 동기화 작업 요약 조회", description = "정책 동기화 작업의 현황 요약 정보를 조회한다.")
    @GetMapping("/summary")
    public ApiResponse<PolicySyncJobSummaryResponse> summary() {
        return ApiResponse.ok(adminPolicySyncJobService.getSummary());
    }
}
