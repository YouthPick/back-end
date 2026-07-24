package com.bop.youthpick.admin.user.service;

import com.bop.youthpick.admin.user.dto.AdminUserProfileResponse;
import com.bop.youthpick.admin.user.dto.AdminUserResponse;
import com.bop.youthpick.admin.user.repository.AdminUserSpecifications;
import com.bop.youthpick.auth.service.RefreshTokenStore;
import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserProfileRepository;
import com.bop.youthpick.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final RefreshTokenStore refreshTokenStore;

    @Transactional(readOnly = true)
    public Page<AdminUserResponse> search(
            String role, String accountStatus, String provider, Pageable pageable) {
        return userRepository
                .findAll(AdminUserSpecifications.filter(role, accountStatus, provider), pageable)
                .map(AdminUserResponse::from);
    }

    @Transactional(readOnly = true)
    public AdminUserProfileResponse getProfile(Long userId) {
        findUser(userId);
        return userProfileRepository
                .findByUserId(userId)
                .map(AdminUserProfileResponse::from)
                .orElse(null);
    }

    @Transactional
    public AdminUserResponse updateRole(Long userId, String role) {
        User user = findUser(userId);
        user.changeRole(Role.valueOf(role));
        return AdminUserResponse.from(user);
    }

    @Transactional
    public AdminUserResponse softDelete(Long userId) {
        User user = findUser(userId);
        user.softDelete();
        // refresh token을 즉시 회수해 제재 실효성을 access token 만료 이내로 확보한다.
        refreshTokenStore.delete(userId);
        return AdminUserResponse.from(user);
    }

    private User findUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
    }
}
