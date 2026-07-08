package com.bop.youthpick.user.dto;

import com.bop.youthpick.user.entity.UserProfile;

public record OnboardingProfileResponse(
        Long id,
        Long userId,
        Integer birthYear,
        String regionCode,
        String employmentStatus,
        String educationLevel,
        String categories,
        String keywords,
        String status
) {
    public static OnboardingProfileResponse from(UserProfile profile) {
        return new OnboardingProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getBirthYear(),
                profile.getRegion().getCode(),
                profile.getEmploymentStatus(),
                profile.getEducationLevel(),
                profile.getCategories(),
                profile.getKeywords(),
                profile.getStatus()
        );
    }
}
