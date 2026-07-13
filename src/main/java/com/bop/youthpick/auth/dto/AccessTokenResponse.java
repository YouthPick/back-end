package com.bop.youthpick.auth.dto;

/** refresh token은 HttpOnly 쿠키로만 내려가므로, 공개 API 응답에는 access token 관련 필드만 노출한다. */
public record AccessTokenResponse(String accessToken, String tokenType, long expiresIn) {

    public static AccessTokenResponse from(TokenResponse tokenResponse) {
        return new AccessTokenResponse(
                tokenResponse.accessToken(), tokenResponse.tokenType(), tokenResponse.expiresIn());
    }
}
