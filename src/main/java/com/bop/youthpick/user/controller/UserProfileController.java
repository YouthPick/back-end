package com.bop.youthpick.user.controller;

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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    // TODO: 인증 도입 후 @PathVariable userId를 인증 principal 기반으로 교체한다.
    @PostMapping("/api/v1/users/{userId}/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> submit(
            @PathVariable Long userId, @Valid @RequestBody UserProfileRequest request) {
        UserProfile profile = userProfileService.submit(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(UserProfileResponse.from(profile)));
    }

    @GetMapping("/api/v1/me/profile")
    public ApiResponse<UserProfileResponse> getMyProfile(@CurrentUser Long userId) {
        UserProfile profile = userProfileService.getMyProfile(userId);
        return ApiResponse.ok(profile == null ? null : UserProfileResponse.from(profile));
    }
}
