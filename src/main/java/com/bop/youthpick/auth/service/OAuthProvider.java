package com.bop.youthpick.auth.service;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import lombok.Getter;

/**
 * 소셜 로그인 provider별 authorize/token/userinfo 엔드포인트. client-id/secret은 {@code OAuthProperties}에서 받는다.
 */
@Getter
public enum OAuthProvider {
    GOOGLE(
            "https://accounts.google.com/o/oauth2/v2/auth",
            "https://oauth2.googleapis.com/token",
            "https://www.googleapis.com/oauth2/v3/userinfo",
            "openid email profile"),
    NAVER(
            "https://nid.naver.com/oauth2.0/authorize",
            "https://nid.naver.com/oauth2.0/token",
            "https://openapi.naver.com/v1/nid/me",
            null),
    KAKAO(
            "https://kauth.kakao.com/oauth/authorize",
            "https://kauth.kakao.com/oauth/token",
            "https://kapi.kakao.com/v2/user/me",
            null);

    private final String authorizationUri;
    private final String tokenUri;
    private final String userInfoUri;
    private final String scope;

    OAuthProvider(String authorizationUri, String tokenUri, String userInfoUri, String scope) {
        this.authorizationUri = authorizationUri;
        this.tokenUri = tokenUri;
        this.userInfoUri = userInfoUri;
        this.scope = scope;
    }

    public static OAuthProvider from(String registrationId) {
        if (registrationId == null) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
        try {
            return OAuthProvider.valueOf(registrationId.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER);
        }
    }

    public String registrationId() {
        return name().toLowerCase();
    }
}
