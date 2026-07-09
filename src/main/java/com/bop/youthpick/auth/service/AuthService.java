package com.bop.youthpick.auth.service;

import com.bop.youthpick.auth.client.OAuthClient;
import com.bop.youthpick.auth.client.OAuthProvider;
import com.bop.youthpick.auth.config.OAuthProperties;
import com.bop.youthpick.auth.dto.OAuthUserInfo;
import com.bop.youthpick.auth.dto.TokenResponse;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.auth.jwt.JwtTokenProvider;
import com.bop.youthpick.auth.jwt.RefreshTokenStore;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
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

    @Transactional
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

        User user =
                userRepository
                        .findByProviderAndProviderId(provider.name(), userInfo.providerId())
                        .orElseGet(
                                () ->
                                        userRepository.save(
                                                User.createSocialUser(
                                                        provider.name(),
                                                        userInfo.providerId(),
                                                        userInfo.email(),
                                                        userInfo.nickname())));

        return issueTokens(user);
    }

    @Transactional
    public TokenResponse refresh(String refreshToken) {
        jwtTokenProvider.validateRefreshToken(refreshToken);
        Long userId = jwtTokenProvider.getUserId(refreshToken);

        String savedRefreshToken =
                refreshTokenStore
                        .find(userId)
                        .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        if (!savedRefreshToken.equals(refreshToken)) {
            throw new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user =
                userRepository
                        .findById(userId)
                        .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHORIZED));
        return issueTokens(user);
    }

    public void logout(Long userId) {
        refreshTokenStore.delete(userId);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHORIZED));
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
