package com.bop.youthpick.auth.dto;

/** provider별 userinfo 응답을 정규화한 값. */
public record OAuthUserInfo(String provider, String providerId, String email, String nickname) {}
