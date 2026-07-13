package com.bop.youthpick.auth.service;

import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code youthpick.oauth.*} 설정 바인딩. client-id/secret은 provider별로 {@code application-local.yml}에서
 * 환경변수로 주입한다.
 *
 * <p>{@code frontendCallbackUri}는 각 provider 콘솔에 등록하는 OAuth redirect_uri와 동일한 프론트엔드 라우트다(예: {@code
 * http://localhost:3000/oauth/callback}). provider는 이 uri로 브라우저를 리다이렉트하고, 프론트가 그 화면에서 code/state를
 * 읽어 백엔드 콜백 API를 호출한다.
 */
@ConfigurationProperties(prefix = "youthpick.oauth")
public record OAuthProperties(String frontendCallbackUri, Map<String, Registration> providers) {

    public record Registration(String clientId, String clientSecret) {

        @Override
        public String toString() {
            return "Registration[clientId=" + clientId + ", clientSecret=****]";
        }
    }
}
