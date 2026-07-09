package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.client.OAuthClient;
import com.bop.youthpick.auth.client.OAuthProvider;
import com.bop.youthpick.auth.config.OAuthProperties;
import com.bop.youthpick.auth.dto.OAuthUserInfo;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.web.context.SecurityContextRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private OAuthClient oAuthClient;
    @Mock private UserRepository userRepository;
    @Mock private SecurityContextRepository securityContextRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        OAuthProperties oAuthProperties =
                new OAuthProperties(
                        "http://localhost:8080",
                        "http://localhost:3000/oauth/callback",
                        Map.of(
                                "google",
                                new OAuthProperties.Registration("client-id", "client-secret")));
        authService =
                new AuthService(
                        oAuthProperties, oAuthClient, userRepository, securityContextRepository);
    }

    @Test
    void 등록된_provider면_state를_세션에_저장하고_인가_url을_생성한다() throws Exception {
        HttpSession session = mock(HttpSession.class);

        String url = authService.buildAuthorizationUrl("google", session);

        Map<String, String> query = parseQuery(url);
        assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(query.get("client_id")).isEqualTo("client-id");
        assertThat(query.get("redirect_uri"))
                .isEqualTo("http://localhost:8080/api/v1/auth/oauth/google/callback");
        assertThat(query.get("state")).isNotBlank();
        verify(session).setAttribute(eq("OAUTH_STATE"), eq(query.get("state")));
    }

    @Test
    void 등록되지_않은_provider면_인가_url_생성시_예외() {
        HttpSession session = mock(HttpSession.class);

        assertThatThrownBy(() -> authService.buildAuthorizationUrl("facebook", session))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void state가_세션값과_다르면_로그인이_거부된다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("OAUTH_STATE")).thenReturn("saved-state");

        assertThatThrownBy(
                        () -> authService.login("google", "code", "other-state", request, response))
                .isInstanceOf(AuthException.class);

        verify(oAuthClient, never()).exchangeCodeForAccessToken(any(), any(), any(), any(), any());
    }

    @Test
    void 신규_사용자면_생성하고_세션에_인증정보를_저장한다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("OAUTH_STATE")).thenReturn("state-value");
        when(oAuthClient.exchangeCodeForAccessToken(
                        eq(OAuthProvider.GOOGLE),
                        eq("client-id"),
                        eq("client-secret"),
                        anyString(),
                        eq("code")))
                .thenReturn("access-token");
        when(oAuthClient.fetchUserInfo(OAuthProvider.GOOGLE, "access-token"))
                .thenReturn(new OAuthUserInfo("GOOGLE", "provider-id-1", "a@a.com", "닉네임"));
        when(userRepository.findByProviderAndProviderId("GOOGLE", "provider-id-1"))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class)))
                .thenReturn(User.createSocialUser("GOOGLE", "provider-id-1", "a@a.com", "닉네임"));

        authService.login("google", "code", "state-value", request, response);

        verify(userRepository).save(any(User.class));
        verify(session).removeAttribute("OAUTH_STATE");
        verify(request).changeSessionId();
        verify(securityContextRepository).saveContext(any(), eq(request), eq(response));
    }

    @Test
    void 기존_사용자면_새로_생성하지_않는다() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        HttpSession session = mock(HttpSession.class);
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute("OAUTH_STATE")).thenReturn("state-value");
        when(oAuthClient.exchangeCodeForAccessToken(
                        eq(OAuthProvider.GOOGLE),
                        eq("client-id"),
                        eq("client-secret"),
                        anyString(),
                        eq("code")))
                .thenReturn("access-token");
        when(oAuthClient.fetchUserInfo(OAuthProvider.GOOGLE, "access-token"))
                .thenReturn(new OAuthUserInfo("GOOGLE", "provider-id-1", "a@a.com", "닉네임"));
        when(userRepository.findByProviderAndProviderId("GOOGLE", "provider-id-1"))
                .thenReturn(
                        Optional.of(
                                User.createSocialUser(
                                        "GOOGLE", "provider-id-1", "a@a.com", "닉네임")));

        authService.login("google", "code", "state-value", request, response);

        verify(userRepository, never()).save(any());
        verify(securityContextRepository).saveContext(any(), eq(request), eq(response));
    }

    private Map<String, String> parseQuery(String url) throws UnsupportedEncodingException {
        String query = URI.create(url).getRawQuery();
        Map<String, String> params = new HashMap<>();
        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            params.put(
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8),
                    URLDecoder.decode(parts[1], StandardCharsets.UTF_8));
        }
        return params;
    }
}
