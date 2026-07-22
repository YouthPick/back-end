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
import com.bop.youthpick.user.dto.UserProfileRequest;
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
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private UserProfileRepository userProfileRepository;

    @Mock private RegionRepository regionRepository;

    private UserProfileService userProfileService;

    private static final Long USER_ID = 1L;
    private static final UserProfileRequest REQUEST =
            new UserProfileRequest(
                    2000,
                    "11110",
                    "EMPLOYED",
                    "UNIVERSITY",
                    "SINGLE",
                    List.of("COMPUTER_SCIENCE"),
                    List.of("LOW_INCOME"),
                    3000,
                    List.of("취업", "주거"),
                    List.of("청년", "공모전"));

    @BeforeEach
    void setUp() {
        userProfileService =
                new UserProfileService(userRepository, userProfileRepository, regionRepository);
    }

    @Test
    void 온보딩_프로필을_정상적으로_생성한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(regionRepository.findById(REQUEST.regionCode()))
                .thenReturn(Optional.of(mock(Region.class)));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        UserProfile result = userProfileService.submit(USER_ID, REQUEST);

        assertThat(result.getBirthYear()).isEqualTo(REQUEST.birthYear());
        assertThat(result.getEmploymentStatus()).isEqualTo(REQUEST.employmentStatus());
        assertThat(result.getEducationLevel()).isEqualTo(REQUEST.educationLevel());
        assertThat(result.getMaritalStatus()).isEqualTo(REQUEST.maritalStatus());
        assertThat(result.getMajor()).isEqualTo("COMPUTER_SCIENCE");
        assertThat(result.getSpecialCondition()).isEqualTo("LOW_INCOME");
        assertThat(result.getIncome()).isEqualTo(REQUEST.income());
        assertThat(result.getCategories()).isEqualTo("취업,주거");
        assertThat(result.getKeywords()).isEqualTo("청년,공모전");
    }

    @Test
    void 존재하지_않는_사용자면_USER_NOT_FOUND_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.submit(USER_ID, REQUEST))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void 이미_프로필이_있으면_PROFILE_ALREADY_EXISTS_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(true);

        assertThatThrownBy(() -> userProfileService.submit(USER_ID, REQUEST))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.PROFILE_ALREADY_EXISTS);
    }

    @Test
    void 저장_시점에_동시요청으로_유니크_제약이_위반되면_PROFILE_ALREADY_EXISTS_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(regionRepository.findById(REQUEST.regionCode()))
                .thenReturn(Optional.of(mock(Region.class)));
        when(userProfileRepository.save(any(UserProfile.class)))
                .thenThrow(new DataIntegrityViolationException("uk_user_profiles_user"));

        assertThatThrownBy(() -> userProfileService.submit(USER_ID, REQUEST))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.PROFILE_ALREADY_EXISTS);
    }

    @Test
    void 프로필이_있으면_조회한다() {
        UserProfile profile = mock(UserProfile.class);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));

        UserProfile result = userProfileService.getMyProfile(USER_ID);

        assertThat(result).isEqualTo(profile);
    }

    @Test
    void 프로필이_없으면_null을_반환한다() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        UserProfile result = userProfileService.getMyProfile(USER_ID);

        assertThat(result).isNull();
    }

    @Test
    void 존재하지_않는_지역코드면_REGION_NOT_FOUND_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.existsByUserId(USER_ID)).thenReturn(false);
        when(regionRepository.findById(REQUEST.regionCode())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.submit(USER_ID, REQUEST))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.REGION_NOT_FOUND);
    }

    @Test
    void 프로필을_정상적으로_수정한다() {
        UserProfile existing =
                UserProfile.create(
                        mock(User.class),
                        mock(Region.class),
                        1998,
                        "UNEMPLOYED",
                        "HIGHSCHOOL",
                        null,
                        null,
                        null,
                        null,
                        null,
                        null);
        Region newRegion = mock(Region.class);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(regionRepository.findById(REQUEST.regionCode())).thenReturn(Optional.of(newRegion));

        UserProfile result = userProfileService.update(USER_ID, REQUEST);

        assertThat(result).isSameAs(existing);
        assertThat(result.getRegion()).isSameAs(newRegion);
        assertThat(result.getBirthYear()).isEqualTo(REQUEST.birthYear());
        assertThat(result.getEmploymentStatus()).isEqualTo(REQUEST.employmentStatus());
        assertThat(result.getEducationLevel()).isEqualTo(REQUEST.educationLevel());
        assertThat(result.getMaritalStatus()).isEqualTo(REQUEST.maritalStatus());
        assertThat(result.getMajor()).isEqualTo("COMPUTER_SCIENCE");
        assertThat(result.getSpecialCondition()).isEqualTo("LOW_INCOME");
        assertThat(result.getIncome()).isEqualTo(REQUEST.income());
        assertThat(result.getCategories()).isEqualTo("취업,주거");
        assertThat(result.getKeywords()).isEqualTo("청년,공모전");
    }

    @Test
    void 수정할_프로필이_없으면_PROFILE_NOT_FOUND_예외를_던진다() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.update(USER_ID, REQUEST))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.PROFILE_NOT_FOUND);
    }

    @Test
    void 수정_시_존재하지_않는_지역코드면_REGION_NOT_FOUND_예외를_던진다() {
        UserProfile existing = mock(UserProfile.class);
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(existing));
        when(regionRepository.findById(REQUEST.regionCode())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.update(USER_ID, REQUEST))
                .isInstanceOf(CustomException.class)
                .extracting(ex -> ((CustomException) ex).getErrorCode())
                .isEqualTo(PolicyErrorCode.REGION_NOT_FOUND);
    }
}
