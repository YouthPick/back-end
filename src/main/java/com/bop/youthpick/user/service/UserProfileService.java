package com.bop.youthpick.user.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.RegionRepository;
import com.bop.youthpick.user.dto.UserProfileRequest;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserProfileRepository;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RegionRepository regionRepository;

    @Transactional(readOnly = true)
    public UserProfile getMyProfile(Long userId) {
        return userProfileRepository.findByUserId(userId).orElse(null);
    }

    @Transactional
    public UserProfile submit(Long userId, UserProfileRequest request) {
        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));

        if (userProfileRepository.existsByUserId(userId)) {
            throw new UserException(UserError.PROFILE_ALREADY_EXISTS);
        }

        Region region =
                regionRepository
                        .findById(request.regionCode())
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.REGION_NOT_FOUND));

        UserProfile profile =
                UserProfile.create(
                        user,
                        region,
                        request.birthYear(),
                        request.employmentStatus(),
                        request.educationLevel(),
                        request.merryStatus(),
                        joinToCommaString(request.major()),
                        joinToCommaString(request.specialCondition()),
                        request.income(),
                        joinToCommaString(request.categories()),
                        joinToCommaString(request.keywords()));

        try {
            return userProfileRepository.save(profile);
        } catch (DataIntegrityViolationException e) {
            // existsByUserId() 확인 이후 다른 트랜잭션이 먼저 저장한 경우
            // (user_profiles.user_id UNIQUE 위반). 같은 도메인 에러로 통일한다.
            throw new UserException(UserError.PROFILE_ALREADY_EXISTS);
        }
    }

    private String joinToCommaString(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join(",", values);
    }
}
