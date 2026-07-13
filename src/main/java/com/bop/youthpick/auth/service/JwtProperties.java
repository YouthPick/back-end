package com.bop.youthpick.auth.service;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code youthpick.jwt.*} 설정 바인딩. secret은 환경변수로 주입하고 코드에 하드코딩하지 않는다. */
@ConfigurationProperties(prefix = "youthpick.jwt")
public record JwtProperties(
        String secret, Duration accessTokenExpiration, Duration refreshTokenExpiration) {}
