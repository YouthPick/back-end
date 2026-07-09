package com.bop.youthpick.policy.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.dto.RegisterManagementRequest;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.service.PolicyManagementService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/managements")
@RequiredArgsConstructor
public class PolicyManagementController {

    private final PolicyManagementService policyManagementService;

    @PostMapping
    public ResponseEntity<ApiResponse<PolicyApplicationResponse>> register(
            @Valid @RequestBody RegisterManagementRequest request) {
        PolicyApplication application =
                policyManagementService.register(
                        request.userId(),
                        request.policyId(),
                        ApplicationStatus.valueOf(request.status()),
                        request.memo(),
                        request.endAt());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(PolicyApplicationResponse.from(application)));
    }

    @GetMapping
    public ApiResponse<List<PolicyApplicationResponse>> getManagements(@RequestParam Long userId) {
        return ApiResponse.ok(policyManagementService.getManagements(userId));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<PolicyApplicationResponse> changeStatus(
            @PathVariable Long id, @RequestParam ApplicationStatus status) {
        PolicyApplication application = policyManagementService.changeStatus(id, status);
        return ApiResponse.ok(PolicyApplicationResponse.from(application));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        policyManagementService.delete(id);
        return ApiResponse.ok(null);
    }
}
