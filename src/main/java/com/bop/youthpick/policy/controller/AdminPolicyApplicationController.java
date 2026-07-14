package com.bop.youthpick.policy.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.AdminPolicyApplicationResponse;
import com.bop.youthpick.policy.dto.AdminPolicyApplicationStatusUpdateRequest;
import com.bop.youthpick.policy.dto.ApplicationChecklistItemResponse;
import com.bop.youthpick.policy.service.AdminPolicyApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/policy-applications")
@RequiredArgsConstructor
@Validated
public class AdminPolicyApplicationController {

    private final AdminPolicyApplicationService adminPolicyApplicationService;

    @GetMapping
    public ApiResponse<List<AdminPolicyApplicationResponse>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String policyName,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "INTERESTED|APPLIED|COMPLETED",
                            message = "status는 INTERESTED, APPLIED, COMPLETED 중 하나여야 합니다.")
                    String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate deadlineStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate deadlineEnd,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminPolicyApplicationResponse> page =
                adminPolicyApplicationService.search(
                        userId, policyName, status, deadlineStart, deadlineEnd, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @GetMapping("/{applicationId}/checklist")
    public ApiResponse<List<ApplicationChecklistItemResponse>> getChecklist(
            @PathVariable Long applicationId) {
        return ApiResponse.ok(adminPolicyApplicationService.getChecklist(applicationId));
    }

    @PatchMapping("/{applicationId}/status")
    public ApiResponse<AdminPolicyApplicationResponse> updateStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody AdminPolicyApplicationStatusUpdateRequest request) {
        return ApiResponse.ok(
                adminPolicyApplicationService.updateStatus(applicationId, request.status()));
    }
}
