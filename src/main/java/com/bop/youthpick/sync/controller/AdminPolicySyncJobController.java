package com.bop.youthpick.sync.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.sync.dto.PolicySyncJobSummaryResponse;
import com.bop.youthpick.sync.service.AdminPolicySyncJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/policy-sync-jobs")
@RequiredArgsConstructor
public class AdminPolicySyncJobController {

    private final AdminPolicySyncJobService adminPolicySyncJobService;

    @GetMapping("/summary")
    public ApiResponse<PolicySyncJobSummaryResponse> summary() {
        return ApiResponse.ok(adminPolicySyncJobService.getSummary());
    }
}
