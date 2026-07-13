package com.bop.youthpick.auth.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code youthpick.auth.refresh-cookie.*} 설정 바인딩. 쿠키 이름은 {@link
 * RefreshTokenCookieSupport#COOKIE_NAME}로 고정하고, 배포 환경마다 달라지는 secure/sameSite/path만 프로필별로
 * override한다.
 */
@ConfigurationProperties(prefix = "youthpick.auth.refresh-cookie")
public record RefreshCookieProperties(String path, boolean secure, String sameSite) {}
