package com.bop.youthpick.user.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.RegionRepository;
import com.bop.youthpick.user.dto.OnboardingProfileRequest;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserProfileRepository;
import com.bop.youthpick.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RegionRepository regionRepository;

    @Transactional
    public UserProfile submit(Long userId, OnboardingProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));

        if (userProfileRepository.existsByUserId(userId)) {
            throw new UserException(UserError.PROFILE_ALREADY_EXISTS);
        }

        Region region = regionRepository.findById(request.regionCode())
                .orElseThrow(() -> new CustomException(PolicyErrorCode.REGION_NOT_FOUND));

        UserProfile profile = UserProfile.create(
                user,
                region,
                request.birthYear(),
                request.employmentStatus(),
                request.educationLevel(),
                request.categories(),
                request.keywords()
        );

        return userProfileRepository.save(profile);
    }
}
