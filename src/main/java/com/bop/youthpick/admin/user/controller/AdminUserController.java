package com.bop.youthpick.admin.user.controller;

import com.bop.youthpick.admin.user.dto.AdminUserProfileResponse;
import com.bop.youthpick.admin.user.dto.AdminUserResponse;
import com.bop.youthpick.admin.user.dto.AdminUserRoleUpdateRequest;
import com.bop.youthpick.admin.user.service.AdminUserService;
import com.bop.youthpick.global.common.ApiResponse;
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

@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
@Validated
public class AdminUserController {

    private final AdminUserService adminUserService;

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

    @GetMapping("/{userId}/profile")
    public ApiResponse<AdminUserProfileResponse> getProfile(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.getProfile(userId));
    }

    @PatchMapping("/{userId}/role")
    public ApiResponse<AdminUserResponse> updateRole(
            @PathVariable Long userId, @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        return ApiResponse.ok(adminUserService.updateRole(userId, request.role()));
    }

    @DeleteMapping("/{userId}")
    public ApiResponse<AdminUserResponse> delete(@PathVariable Long userId) {
        return ApiResponse.ok(adminUserService.softDelete(userId));
    }
}
