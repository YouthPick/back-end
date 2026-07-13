package com.bop.youthpick.policy.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.dto.RegisterPolicyApplicationRequest;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.service.PolicyApplicationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class PolicyApplicationController {

    private final PolicyApplicationService policyApplicationService;

    @PostMapping
    public ResponseEntity<ApiResponse<PolicyApplicationResponse>> register(
            @CurrentUser Long userId,
            @Valid @RequestBody RegisterPolicyApplicationRequest request) {
        PolicyApplication application =
                policyApplicationService.register(
                        userId,
                        request.policyId(),
                        ApplicationStatus.valueOf(request.status()),
                        request.memo(),
                        request.endAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PolicyApplicationResponse.from(application)));
    }

    @GetMapping
    public ApiResponse<List<PolicyApplicationResponse>> getApplications(
            @CurrentUser Long userId, @PageableDefault(size = 20) Pageable pageable) {
        Page<PolicyApplicationResponse> page =
                policyApplicationService.getApplications(userId, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PolicyApplicationResponse> changeStatus(
            @CurrentUser Long userId,
            @PathVariable Long id,
            @RequestParam
                    @NotBlank(message = "상태는 필수입니다.")
                    @Pattern(regexp = "INTERESTED|APPLIED|COMPLETED", message = "유효하지 않은 상태값입니다.")
                    String status) {
        PolicyApplication application =
                policyApplicationService.changeStatus(
                        id, userId, ApplicationStatus.valueOf(status));
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@CurrentUser Long userId, @PathVariable Long id) {
        policyApplicationService.delete(id, userId);
        return ApiResponse.ok(null);
    }
}
