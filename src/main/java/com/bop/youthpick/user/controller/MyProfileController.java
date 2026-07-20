package com.bop.youthpick.user.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.user.dto.OnboardingProfileResponse;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.service.OnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class MyProfileController {

    private final OnboardingService onboardingService;

    @GetMapping("/profile")
    public ApiResponse<OnboardingProfileResponse> getProfile(@CurrentUser Long userId) {
        UserProfile profile = onboardingService.getMyProfile(userId);
        return ApiResponse.ok(profile == null ? null : OnboardingProfileResponse.from(profile));
    }
}
