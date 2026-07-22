package com.bop.youthpick.admin.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.service.RefreshTokenStore;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserProfileRepository;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private UserProfileRepository userProfileRepository;

    @Mock private RefreshTokenStore refreshTokenStore;

    private AdminUserService adminUserService;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        adminUserService =
                new AdminUserService(userRepository, userProfileRepository, refreshTokenStore);
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_페이지를_반환한다() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(USER_ID);
        when(user.getProvider()).thenReturn("kakao");
        when(user.getProviderId()).thenReturn("kakao-1");
        when(user.getRole()).thenReturn(Role.USER);
        Page<User> page = new PageImpl<>(java.util.List.of(user));
        when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<?> result = adminUserService.search("USER", "ACTIVE", "kakao", Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(userRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void 프로필이_있으면_프로필을_반환한다() {
        User user = mock(User.class);
        UserProfile profile = mock(UserProfile.class);
        Region region = mock(Region.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(profile.getUser()).thenReturn(user);
        when(user.getId()).thenReturn(USER_ID);
        when(profile.getRegion()).thenReturn(region);
        when(region.getSidoName()).thenReturn("서울특별시");
        when(region.getName()).thenReturn("강남구");
        when(profile.getMerryStatus()).thenReturn("SINGLE");
        when(profile.getMajor()).thenReturn("COMPUTER_SCIENCE");
        when(profile.getSpecialCondition()).thenReturn("LOW_INCOME");
        when(profile.getIncome()).thenReturn(3000);

        var result = adminUserService.getProfile(USER_ID);

        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.regionLabel()).isEqualTo("서울특별시 강남구");
        assertThat(result.merryStatus()).isEqualTo("SINGLE");
        assertThat(result.major()).containsExactly("COMPUTER_SCIENCE");
        assertThat(result.specialCondition()).containsExactly("LOW_INCOME");
        assertThat(result.income()).isEqualTo(3000);
    }

    @Test
    void 프로필이_없으면_null을_반환한다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(mock(User.class)));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        var result = adminUserService.getProfile(USER_ID);

        assertThat(result).isNull();
    }

    @Test
    void 프로필_조회_대상_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.getProfile(USER_ID))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void role을_변경한다() {
        User user = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(USER_ID);
        when(user.getRole()).thenReturn(Role.ADMIN);

        var result = adminUserService.updateRole(USER_ID, "ADMIN");

        verify(user).changeRole(Role.ADMIN);
        assertThat(result.role()).isEqualTo(Role.ADMIN);
    }

    @Test
    void role_변경_대상_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.updateRole(USER_ID, "ADMIN"))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.USER_NOT_FOUND);
    }

    @Test
    void 회원을_soft_delete_한다() {
        User user = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(USER_ID);

        adminUserService.softDelete(USER_ID);

        verify(user).softDelete();
    }

    @Test
    void soft_delete_시_refresh_token을_즉시_회수한다() {
        User user = mock(User.class);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(user.getId()).thenReturn(USER_ID);

        adminUserService.softDelete(USER_ID);

        verify(refreshTokenStore).delete(USER_ID);
    }

    @Test
    void 탈퇴_대상_사용자가_없으면_USER_NOT_FOUND_예외를_던진다() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminUserService.softDelete(USER_ID))
                .isInstanceOf(UserException.class)
                .extracting(ex -> ((UserException) ex).getErrorCode())
                .isEqualTo(UserError.USER_NOT_FOUND);
    }
}
