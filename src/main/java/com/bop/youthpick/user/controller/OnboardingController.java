package com.bop.youthpick.user.controller;

import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.user.dto.OnboardingProfileRequest;
import com.bop.youthpick.user.dto.OnboardingProfileResponse;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.service.OnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class OnboardingController {

    private final OnboardingService onboardingService;

    // TODO: 인증 도입 후 @PathVariable userId를 인증 principal 기반으로 교체한다.
    @PostMapping("/{userId}/profile")
    public ResponseEntity<ApiResponse<OnboardingProfileResponse>> submit(
            @PathVariable Long userId, @Valid @RequestBody OnboardingProfileRequest request) {
        UserProfile profile = onboardingService.submit(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(OnboardingProfileResponse.from(profile)));
    }
}
