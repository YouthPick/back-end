package com.bop.youthpick.admin.user.controller;

import com.bop.youthpick.admin.user.dto.AdminUserProfileResponse;
import com.bop.youthpick.admin.user.dto.AdminUserResponse;
import com.bop.youthpick.admin.user.dto.AdminUserRoleUpdateRequest;
import com.bop.youthpick.admin.user.service.AdminUserService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 회원")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "회원 목록 조회", description = "권한, 계정 상태, 가입 경로로 회원을 검색해 페이지 단위로 조회합니다.")
    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list(
            @RequestParam(required = false)
                    @Pattern(regexp = "USER|ADMIN", message = "role은 USER 또는 ADMIN이어야 합니다.")
                    String role,
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "ACTIVE|DELETED",
                            message = "accountStatus는 ACTIVE 또는 DELETED여야 합니다.")
                    String accountStatus,
            @RequestParam(required = false) String provider,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminUserResponse> page =
                adminUserService.search(role, accountStatus, provider, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @Operation(summary = "회원 프로필 조회", description = "지정한 회원의 상세 프로필을 조회합니다.")
    @GetMapping("/{userId}/profile")
    public ApiResponse<AdminUserProfileResponse> getProfile(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.getProfile(userId));
    }

    @Operation(summary = "회원 권한 변경", description = "관리자가 지정한 회원의 권한(role)을 변경합니다.")
    @PatchMapping("/{userId}/role")
    public ApiResponse<AdminUserResponse> updateRole(
            @PathVariable Long userId, @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        return ApiResponse.ok(adminUserService.updateRole(userId, request.role()));
    }

    @Operation(summary = "회원 강제 탈퇴", description = "관리자가 지정한 회원을 소프트 삭제(강제 탈퇴) 처리합니다.")
    @DeleteMapping("/{userId}")
    public ApiResponse<AdminUserResponse> delete(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.softDelete(userId));
    }
}
