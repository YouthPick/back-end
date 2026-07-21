package com.bop.youthpick.user.dto;

import com.bop.youthpick.user.entity.UserProfile;
import java.util.Arrays;
import java.util.List;

public record UserProfileResponse(
        Long id,
        Long userId,
        Integer birthYear,
        String regionCode,
        String employmentStatus,
        String educationLevel,
        String merryStatus,
        List<String> major,
        List<String> specialCondition,
        Integer income,
        List<String> categories,
        List<String> keywords,
        String status) {
    public static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUser().getId(),
                profile.getBirthYear(),
                profile.getRegion().getCode(),
                profile.getEmploymentStatus(),
                profile.getEducationLevel(),
                profile.getMerryStatus(),
                splitToList(profile.getMajor()),
                splitToList(profile.getSpecialCondition()),
                profile.getIncome(),
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
