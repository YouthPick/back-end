package com.bop.youthpick.auth.client;

import com.bop.youthpick.auth.dto.OAuthUserInfo;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** OAuth2 authorization code를 access token으로 교환하고, provider별 userinfo를 정규화해 조회한다. */
@Component
public class OAuthClient {

    private final RestClient restClient;

    public OAuthClient(RestClient.Builder restClientBuilder) {
        this.restClient = restClientBuilder.build();
    }

    public String exchangeCodeForAccessToken(
            OAuthProvider provider,
            String clientId,
            String clientSecret,
            String redirectUri,
            String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);

        JsonNode response = post(provider.getTokenUri(), form);
        JsonNode accessToken = response.get("access_token");
        if (accessToken == null || accessToken.asText().isBlank()) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }
        return accessToken.asText();
    }

    public OAuthUserInfo fetchUserInfo(OAuthProvider provider, String accessToken) {
        JsonNode response;
        try {
            response =
                    restClient
                            .get()
                            .uri(provider.getUserInfoUri())
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                            .retrieve()
                            .body(JsonNode.class);
        } catch (RestClientException e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }
        if (response == null) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }

        return switch (provider) {
            case GOOGLE -> parseGoogle(response);
            case NAVER -> parseNaver(response);
            case KAKAO -> parseKakao(response);
        };
    }

    private JsonNode post(String uri, MultiValueMap<String, String> form) {
        try {
            JsonNode response =
                    restClient
                            .post()
                            .uri(uri)
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .accept(MediaType.APPLICATION_JSON)
                            .body(form)
                            .retrieve()
                            .body(JsonNode.class);
            if (response == null) {
                throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
            }
            return response;
        } catch (RestClientException e) {
            throw new AuthException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }

    private OAuthUserInfo parseGoogle(JsonNode node) {
        return new OAuthUserInfo(
                OAuthProvider.GOOGLE.name(),
                node.path("sub").asText(),
                textOrNull(node.path("email")),
                textOrNull(node.path("name")));
    }

    private OAuthUserInfo parseNaver(JsonNode node) {
        JsonNode account = node.path("response");
        return new OAuthUserInfo(
                OAuthProvider.NAVER.name(),
                account.path("id").asText(),
                textOrNull(account.path("email")),
                textOrNull(account.path("name")));
    }

    private OAuthUserInfo parseKakao(JsonNode node) {
        JsonNode kakaoAccount = node.path("kakao_account");
        JsonNode profile = kakaoAccount.path("profile");
        return new OAuthUserInfo(
                OAuthProvider.KAKAO.name(),
                node.path("id").asText(),
                textOrNull(kakaoAccount.path("email")),
                textOrNull(profile.path("nickname")));
    }

    private String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
