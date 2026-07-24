package com.bop.youthpick.admin.user.dto;

import com.bop.youthpick.user.entity.UserProfile;
import java.util.Arrays;
import java.util.List;

public record AdminUserProfileResponse(
        Long userId,
        Integer birthYear,
        String employmentStatus,
        String educationLevel,
        String maritalStatus,
        List<String> major,
        List<String> specialCondition,
        Integer income,
        List<String> categories,
        List<String> keywords,
        String status,
        String regionLabel) {
    public static AdminUserProfileResponse from(UserProfile profile) {
        return new AdminUserProfileResponse(
                profile.getUser().getId(),
                profile.getBirthYear(),
                profile.getEmploymentStatus(),
                profile.getEducationLevel(),
                profile.getMaritalStatus(),
                splitToList(profile.getMajor()),
                splitToList(profile.getSpecialCondition()),
                profile.getIncome(),
                splitToList(profile.getCategories()),
                splitToList(profile.getKeywords()),
                profile.getStatus(),
                profile.getRegion().getSidoName() + " " + profile.getRegion().getName());
    }

    private static List<String> splitToList(String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return List.of();
        }
        return Arrays.asList(commaSeparated.split(","));
    }
}
