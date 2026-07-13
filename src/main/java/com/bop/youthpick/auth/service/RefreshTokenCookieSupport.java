package com.bop.youthpick.auth.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * refresh token을 HttpOnly 쿠키로 발급/삭제한다. access token은 이전처럼 응답 body로 내려간다. 크로스사이트 배포(SameSite=None)와
 * 로컬(SameSite=Lax, http)의 secure/sameSite 차이는 {@link RefreshCookieProperties}로 프로필별 override한다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenCookieSupport {

    public static final String COOKIE_NAME = "refresh_token";

    private final RefreshCookieProperties properties;
    private final JwtProperties jwtProperties;

    public ResponseCookie issue(String refreshToken) {
        return build(refreshToken, jwtProperties.refreshTokenExpiration());
    }

    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    private ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(properties.secure())
                .sameSite(properties.sameSite())
                .path(properties.path())
                .maxAge(maxAge)
                .build();
    }
}
