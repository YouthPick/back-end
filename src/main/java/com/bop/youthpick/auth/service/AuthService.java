package com.bop.youthpick.auth.service;

import com.bop.youthpick.auth.dto.OAuthUserInfo;
import com.bop.youthpick.auth.dto.TokenResponse;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.user.entity.LoginHistory;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.LoginHistoryRepository;
import com.bop.youthpick.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * 카카오/구글/네이버 OAuth2 authorization code 로그인 흐름. 인증은 JWT access/refresh token으로 발급하며, refresh token은
 * {@code RefreshTokenStore}(Redis, TTL)로 관리한다.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final OAuthProperties oAuthProperties;
    private final OAuthClient oAuthClient;
    private final OAuthStateStore oAuthStateStore;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final LoginHistoryRepository loginHistoryRepository;

    public String buildAuthorizationUrl(String registrationId) {
        OAuthProvider provider = OAuthProvider.from(registrationId);
        OAuthProperties.Registration registration = registration(provider);

        String state = UUID.randomUUID().toString();
        oAuthStateStore.save(state, provider.name());

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
                        .queryParam("response_type", "code")
                        .queryParam("client_id", registration.clientId())
                        .queryParam("redirect_uri", oAuthProperties.frontendCallbackUri())
                        .queryParam("state", state);
        if (StringUtils.hasText(provider.getScope())) {
            builder.queryParam("scope", provider.getScope());
        }
        return builder.encode().build().toUriString();
    }

    /**
     * DB 저장 없이 API 호출만: provider 통신(state 검증 이후)은 트랜잭션 밖에서 수행해 네트워크 지연 동안 DB 커넥션을 점유하지 않는다. 실제 DB
     * 쓰기(find-or-create)는 {@link #findOrCreateUser}의 각 리포지토리 호출이 Spring Data JPA 기본 트랜잭션으로 처리한다.
     */
    public TokenResponse login(String registrationId, String code, String state) {
        OAuthProvider provider = OAuthProvider.from(registrationId);
        if (!oAuthStateStore.consume(state, provider.name())) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH_STATE);
        }

        OAuthProperties.Registration registration = registration(provider);
        String accessToken =
                oAuthClient.exchangeCodeForAccessToken(
                        provider,
                        registration.clientId(),
                        registration.clientSecret(),
                        oAuthProperties.frontendCallbackUri(),
                        code);
        OAuthUserInfo userInfo = oAuthClient.fetchUserInfo(provider, accessToken);

        User user = findOrCreateUser(provider, userInfo);
        loginHistoryRepository.save(LoginHistory.create(user));
        return issueTokens(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        Claims claims = jwtTokenProvider.validateRefreshToken(refreshToken);
        Long userId = jwtTokenProvider.getUserId(claims);

        // soft-delete(탈퇴/제재)된 사용자는 refresh token TTL이 남아 있어도 재발급을 거부한다.
        User user =
                userRepository
                        .findByIdAndDeletedAtIsNull(userId)
                        .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHORIZED));

        if (!refreshTokenStore.matches(userId, refreshToken)) {
            // 저장된 refresh token과 불일치(로그아웃되었거나 유효하지 않음)
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        String newAccessToken =
                jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        return TokenResponse.of(
                newAccessToken, refreshToken, jwtTokenProvider.accessTokenExpirationSeconds());
    }

    // Redis(refresh token 삭제)만 다루고 JPA 트랜잭션 리소스를 쓰지 않아 @Transactional 대상이 아니다.
    public void logout(Long userId) {
        refreshTokenStore.delete(userId);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(Long userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHORIZED));
    }

    private User findOrCreateUser(OAuthProvider provider, OAuthUserInfo userInfo) {
        User user =
                userRepository
                        .findByProviderAndProviderId(provider.name(), userInfo.providerId())
                        .orElseGet(() -> createUser(provider, userInfo));
        // 탈퇴/제재(soft-delete)된 계정은 재로그인을 거부한다(재활성 아님, 팀 리뷰 결정사항).
        if (user.getDeletedAt() != null) {
            throw new AuthException(AuthErrorCode.ACCOUNT_DISABLED);
        }
        return user;
    }

    private User createUser(OAuthProvider provider, OAuthUserInfo userInfo) {
        try {
            return userRepository.save(
                    User.createSocialUser(
                            provider.name(),
                            userInfo.providerId(),
                            userInfo.email(),
                            userInfo.nickname()));
        } catch (DataIntegrityViolationException e) {
            // uk_users_provider 위반: 동시 콜백 요청이 먼저 저장한 경우, 그 사용자로 재조회해 복구한다.
            return userRepository
                    .findByProviderAndProviderId(provider.name(), userInfo.providerId())
                    .orElseThrow(() -> e);
        }
    }

    private TokenResponse issueTokens(User user) {
        String accessToken =
                jwtTokenProvider.createAccessToken(user.getId(), user.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());
        refreshTokenStore.save(
                user.getId(), refreshToken, jwtTokenProvider.refreshTokenExpiration());
        return TokenResponse.of(
                accessToken, refreshToken, jwtTokenProvider.accessTokenExpirationSeconds());
    }

    private OAuthProperties.Registration registration(OAuthProvider provider) {
        OAuthProperties.Registration registration =
                oAuthProperties.providers().get(provider.registrationId());
        if (registration == null || !StringUtils.hasText(registration.clientId())) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        return registration;
    }
}
