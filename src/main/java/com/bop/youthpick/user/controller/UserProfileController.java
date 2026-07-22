package com.bop.youthpick.user.controller;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.user.dto.UserProfileRequest;
import com.bop.youthpick.user.dto.UserProfileResponse;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    // 경로의 userId는 프론트 호환을 위해 유지하되, 실제 대상은 인증 principal이다.
    // path와 principal이 다르면 타인 프로필 생성(IDOR)이므로 403으로 거부한다.
    @PostMapping("/api/v1/users/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> submit(
            @CurrentUser Long currentUserId,
            @PathVariable Long userId,
            @Valid @RequestBody UserProfileRequest request) {
        if (!userId.equals(currentUserId)) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
        UserProfile profile = userProfileService.submit(currentUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UserProfileResponse.from(profile)));
    }

    @GetMapping("/api/v1/me/profile")
    public ApiResponse<UserProfileResponse> getMyProfile(@CurrentUser Long userId) {
        UserProfile profile = userProfileService.getMyProfile(userId);
        return ApiResponse.ok(profile == null ? null : UserProfileResponse.from(profile));
    }

    @PatchMapping("/api/v1/me/profile")
    public ApiResponse<UserProfileResponse> updateMyProfile(
            @CurrentUser Long userId, @Valid @RequestBody UserProfileRequest request) {
        UserProfile profile = userProfileService.update(userId, request);
        return ApiResponse.ok(UserProfileResponse.from(profile));
    }
}
