package com.bop.youthpick.user.dto;

import com.bop.youthpick.user.entity.UserProfile;
import java.util.Arrays;
import java.util.List;

public record OnboardingProfileResponse(
        Long id,
        Long userId,
        Integer birthYear,
        String regionCode,
        String employmentStatus,
        String educationLevel,
        List<String> categories,
        List<String> keywords,
        String status) {
    public static OnboardingProfileResponse from(UserProfile profile) {
        return new OnboardingProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getBirthYear(),
                profile.getRegion().getCode(),
                profile.getEmploymentStatus(),
                profile.getEducationLevel(),
                splitToList(profile.getCategories()),
                splitToList(profile.getKeywords()),
                profile.getStatus());
    }

    private static List<String> splitToList(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }
        return Arrays.asList(commaSeparated.split(","));
    }
}
