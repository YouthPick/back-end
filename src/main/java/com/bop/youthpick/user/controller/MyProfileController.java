package com.bop.youthpick.user.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.user.dto.UserProfileResponse;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MyProfileController {

    private final UserProfileService userProfileService;

    @GetMapping("/profile")
    public ApiResponse<UserProfileResponse> getProfile(@CurrentUser Long userId) {
        UserProfile profile = userProfileService.getMyProfile(userId);
        return ApiResponse.ok(profile == null ? null : UserProfileResponse.from(profile));
    }
}
