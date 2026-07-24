package com.bop.youthpick.admin.policy.controller;

import com.bop.youthpick.admin.policy.dto.AdminPolicyResponse;
import com.bop.youthpick.admin.policy.dto.AdminPolicyUpdateRequest;
import com.bop.youthpick.admin.policy.dto.AdminPolicyVisibilityUpdateRequest;
import com.bop.youthpick.admin.policy.service.AdminPolicyService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 정책")
@RestController
@RequestMapping("/api/v1/admin/policies")
@RequiredArgsConstructor
@Validated
public class AdminPolicyController {

    private final AdminPolicyService adminPolicyService;

    @Operation(summary = "정책 목록 조회", description = "카테고리, 노출 상태, 기간으로 정책 목록을 검색하여 페이지 단위로 조회한다.")
    @GetMapping
    public ApiResponse<List<AdminPolicyResponse>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "VISIBLE|HIDDEN",
                            message = "visibilityStatus는 VISIBLE 또는 HIDDEN이어야 합니다.")
                    String visibilityStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminPolicyResponse> page =
                adminPolicyService.search(category, visibilityStatus, startDate, endDate, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @Operation(summary = "정책 정보 수정", description = "특정 정책의 정보를 수정한다.")
    @PutMapping("/{policyId}")
    public ApiResponse<AdminPolicyResponse> update(
            @PathVariable Long policyId, @Valid @RequestBody AdminPolicyUpdateRequest request) {
        return ApiResponse.ok(adminPolicyService.update(policyId, request));
    }

    @Operation(summary = "정책 노출 상태 변경", description = "특정 정책의 노출 상태(VISIBLE/HIDDEN)를 변경한다.")
    @PatchMapping("/{policyId}/visibility")
    public ApiResponse<AdminPolicyResponse> updateVisibility(
            @PathVariable Long policyId,
            @Valid @RequestBody AdminPolicyVisibilityUpdateRequest request) {
        return ApiResponse.ok(
                adminPolicyService.updateVisibility(policyId, request.visibilityStatus()));
    }

    @Operation(summary = "정책 삭제", description = "특정 정책을 소프트 삭제 처리한다.")
    @DeleteMapping("/{policyId}")
    public ApiResponse<AdminPolicyResponse> delete(@PathVariable Long policyId) {
        return ApiResponse.ok(adminPolicyService.softDelete(policyId));
    }
}
