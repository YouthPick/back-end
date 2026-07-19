package com.bop.youthpick.admin.sync.controller;

import com.bop.youthpick.admin.sync.dto.PolicySyncJobSummaryResponse;
import com.bop.youthpick.admin.sync.service.AdminPolicySyncJobService;
import com.bop.youthpick.global.common.ApiResponse;
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
