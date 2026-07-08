package com.bop.youthpick.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserProfileRepository userProfileRepository;

    @Mock
    private RegionRepository regionRepository;

    private OnboardingService onboardingService;

    private static final Long USER_ID = 1L;
    private static final OnboardingProfileRequest REQUEST = new OnboardingProfileRequest(
            2000, "11110", "EMPLOYED", "UNIVERSITY",
            List.of("취업", "주거"), List.of("청년", "공모전")
    );

    @BeforeEach
    void setUp() {
        onboardingService = new OnboardingService(userRepository, userProfileRepository, regionRepository);
    }

    @Test
    void 온보딩_프로필을_정상적으로_생성한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(regionRepository.findById(REQUEST.regionCode())).thenReturn(Optional.of(mock(Region.class)));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = onboardingService.submit(USER_ID, REQUEST);

        assertThat(result.getBirthYear()).isEqualTo(REQUEST.birthYear());
        assertThat(result.getEmploymentStatus()).isEqualTo(REQUEST.employmentStatus());
        assertThat(result.getCategories()).isEqualTo("취업,주거");
        assertThat(result.getKeywords()).isEqualTo("청년,공모전");
    }

    @Test
    void 존재하지_않는_사용자면_USER_NOT_FOUND_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.submit(USER_ID, REQUEST))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void 이미_프로필이_있으면_PROFILE_ALREADY_EXISTS_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> onboardingService.submit(USER_ID, REQUEST))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.PROFILE_ALREADY_EXISTS);
    }

    @Test
    void 존재하지_않는_지역코드면_REGION_NOT_FOUND_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(regionRepository.findById(REQUEST.regionCode())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> onboardingService.submit(USER_ID, REQUEST))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.REGION_NOT_FOUND);
    }
}
