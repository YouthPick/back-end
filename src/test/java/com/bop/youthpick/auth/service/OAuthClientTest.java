package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.bop.youthpick.auth.dto.OAuthUserInfo;
import com.bop.youthpick.auth.exception.AuthException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OAuthClientTest {

    @Test
    void 토큰_교환_성공시_access_token을_반환한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(OAuthProvider.GOOGLE.getTokenUri()))
                .andExpect(method(HttpMethod.POST))
                .andRespond(
                        withSuccess("{\"access_token\":\"token-1\"}", MediaType.APPLICATION_JSON));
        OAuthClient client = new OAuthClient(builder);

        String accessToken =
                client.exchangeCodeForAccessToken(
                        OAuthProvider.GOOGLE, "id", "secret", "http://localhost/callback", "code");

        assertThat(accessToken).isEqualTo("token-1");
    }

    @Test
    void 토큰_응답에_access_token이_없으면_예외() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(OAuthProvider.GOOGLE.getTokenUri()))
                .andRespond(
                        withSuccess("{\"error\":\"invalid_grant\"}", MediaType.APPLICATION_JSON));
        OAuthClient client = new OAuthClient(builder);

        assertThatThrownBy(
                        () ->
                                client.exchangeCodeForAccessToken(
                                        OAuthProvider.GOOGLE,
                                        "id",
                                        "secret",
                                        "http://localhost/callback",
                                        "code"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void provider_통신_실패시_예외로_변환한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(OAuthProvider.KAKAO.getUserInfoUri()))
                .andRespond(withServerError());
        OAuthClient client = new OAuthClient(builder);

        assertThatThrownBy(() -> client.fetchUserInfo(OAuthProvider.KAKAO, "token"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void 구글_userinfo_응답을_정규화한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String body =
                """
                {
                  "sub": "google-1",
                  "email": "a@a.com",
                  "name": "이름"
                }
                """;
        server.expect(requestTo(OAuthProvider.GOOGLE.getUserInfoUri()))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        OAuthClient client = new OAuthClient(builder);

        OAuthUserInfo userInfo = client.fetchUserInfo(OAuthProvider.GOOGLE, "token");

        assertThat(userInfo.provider()).isEqualTo("GOOGLE");
        assertThat(userInfo.providerId()).isEqualTo("google-1");
        assertThat(userInfo.email()).isEqualTo("a@a.com");
        assertThat(userInfo.nickname()).isEqualTo("이름");
    }

    @Test
    void 카카오_userinfo_응답을_정규화한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String body =
                """
                {
                  "id": 12345,
                  "kakao_account": {
                    "email": "a@a.com",
                    "profile": { "nickname": "닉네임" }
                  }
                }
                """;
        server.expect(requestTo(OAuthProvider.KAKAO.getUserInfoUri()))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        OAuthClient client = new OAuthClient(builder);

        OAuthUserInfo userInfo = client.fetchUserInfo(OAuthProvider.KAKAO, "token");

        assertThat(userInfo.provider()).isEqualTo("KAKAO");
        assertThat(userInfo.providerId()).isEqualTo("12345");
        assertThat(userInfo.email()).isEqualTo("a@a.com");
        assertThat(userInfo.nickname()).isEqualTo("닉네임");
    }

    @Test
    void userinfo_응답에_providerId가_없으면_예외() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String body =
                """
                {
                  "kakao_account": { "email": "a@a.com" }
                }
                """;
        server.expect(requestTo(OAuthProvider.KAKAO.getUserInfoUri()))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        OAuthClient client = new OAuthClient(builder);

        assertThatThrownBy(() -> client.fetchUserInfo(OAuthProvider.KAKAO, "token"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void 네이버_userinfo_응답을_정규화한다() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        String body =
                """
                {
                  "response": { "id": "naver-1", "email": "b@b.com", "name": "이름" }
                }
                """;
        server.expect(requestTo(OAuthProvider.NAVER.getUserInfoUri()))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
        OAuthClient client = new OAuthClient(builder);

        OAuthUserInfo userInfo = client.fetchUserInfo(OAuthProvider.NAVER, "token");

        assertThat(userInfo.provider()).isEqualTo("NAVER");
        assertThat(userInfo.providerId()).isEqualTo("naver-1");
        assertThat(userInfo.email()).isEqualTo("b@b.com");
        assertThat(userInfo.nickname()).isEqualTo("이름");
    }
}
