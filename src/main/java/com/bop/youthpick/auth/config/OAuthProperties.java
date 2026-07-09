package com.bop.youthpick.auth.config;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code youthpick.oauth.*} 설정 바인딩. client-id/secret은 provider별로 {@code application-local.yml}에서
 * 환경변수로 주입한다.
 */
@ConfigurationProperties(prefix = "youthpick.oauth")
public record OAuthProperties(
        String redirectBaseUri, String frontendRedirectUri, Map<String, Registration> providers) {

    public record Registration(String clientId, String clientSecret) {}
}
