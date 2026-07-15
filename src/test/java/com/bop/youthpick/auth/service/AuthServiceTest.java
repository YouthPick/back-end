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

import com.bop.youthpick.auth.dto.OAuthUserInfo;
import com.bop.youthpick.auth.dto.TokenResponse;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.user.entity.LoginHistory;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.LoginHistoryRepository;
import com.bop.youthpick.user.repository.UserRepository;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private OAuthClient oAuthClient;
    @Mock private OAuthStateStore oAuthStateStore;
    @Mock private UserRepository userRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenStore refreshTokenStore;
    @Mock private LoginHistoryRepository loginHistoryRepository;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        OAuthProperties oAuthProperties =
                new OAuthProperties(
                        "http://localhost:3000/oauth/callback",
                        Map.of(
                                "google",
                                new OAuthProperties.Registration("client-id", "client-secret")));
        authService =
                new AuthService(
                        oAuthProperties,
                        oAuthClient,
                        oAuthStateStore,
                        userRepository,
                        jwtTokenProvider,
                        refreshTokenStore,
                        loginHistoryRepository);
    }

    @Test
    void 등록된_provider면_state를_저장하고_인가_url을_생성한다() throws Exception {
        String url = authService.buildAuthorizationUrl("google");

        Map<String, String> query = parseQuery(url);
        assertThat(url).startsWith("https://accounts.google.com/o/oauth2/v2/auth");
        assertThat(query.get("client_id")).isEqualTo("client-id");
        assertThat(query.get("redirect_uri")).isEqualTo("http://localhost:3000/oauth/callback");
        assertThat(query.get("state")).isNotBlank();
        verify(oAuthStateStore).save(eq(query.get("state")), eq("GOOGLE"));
    }

    @Test
    void 등록되지_않은_provider면_인가_url_생성시_예외() {
        assertThatThrownBy(() -> authService.buildAuthorizationUrl("facebook"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void state_검증에_실패하면_로그인이_거부된다() {
        when(oAuthStateStore.consume("bad-state", "GOOGLE")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("google", "code", "bad-state"))
                .isInstanceOf(AuthException.class);

        verify(oAuthClient, never()).exchangeCodeForAccessToken(any(), any(), any(), any(), any());
        verify(loginHistoryRepository, never()).save(any());
    }

    @Test
    void 신규_사용자면_생성하고_토큰을_발급한다() {
        when(oAuthStateStore.consume("state-value", "GOOGLE")).thenReturn(true);
        when(oAuthClient.exchangeCodeForAccessToken(
                        eq(OAuthProvider.GOOGLE),
                        eq("client-id"),
                        eq("client-secret"),
                        anyString(),
                        eq("code")))
                .thenReturn("provider-access-token");
        when(oAuthClient.fetchUserInfo(OAuthProvider.GOOGLE, "provider-access-token"))
                .thenReturn(new OAuthUserInfo("GOOGLE", "provider-id-1", "a@a.com", "닉네임"));
        when(userRepository.findByProviderAndProviderId("GOOGLE", "provider-id-1"))
                .thenReturn(Optional.empty());
        User savedUser = User.createSocialUser("GOOGLE", "provider-id-1", "a@a.com", "닉네임");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.createAccessToken(any(), any())).thenReturn("jwt-access");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("jwt-refresh");
        when(jwtTokenProvider.refreshTokenExpiration()).thenReturn(Duration.ofDays(14));
        when(jwtTokenProvider.accessTokenExpirationSeconds()).thenReturn(1800L);

        TokenResponse tokens = authService.login("google", "code", "state-value");

        verify(userRepository).save(any(User.class));
        verify(refreshTokenStore).save(savedUser.getId(), "jwt-refresh", Duration.ofDays(14));
        verify(loginHistoryRepository).save(any(LoginHistory.class));
        assertThat(tokens.accessToken()).isEqualTo("jwt-access");
        assertThat(tokens.refreshToken()).isEqualTo("jwt-refresh");
        assertThat(tokens.expiresIn()).isEqualTo(1800L);
    }

    @Test
    void 기존_사용자면_새로_생성하지_않는다() {
        when(oAuthStateStore.consume("state-value", "GOOGLE")).thenReturn(true);
        when(oAuthClient.exchangeCodeForAccessToken(
                        eq(OAuthProvider.GOOGLE),
                        eq("client-id"),
                        eq("client-secret"),
                        anyString(),
                        eq("code")))
                .thenReturn("provider-access-token");
        when(oAuthClient.fetchUserInfo(OAuthProvider.GOOGLE, "provider-access-token"))
                .thenReturn(new OAuthUserInfo("GOOGLE", "provider-id-1", "a@a.com", "닉네임"));
        when(userRepository.findByProviderAndProviderId("GOOGLE", "provider-id-1"))
                .thenReturn(
                        Optional.of(
                                User.createSocialUser(
                                        "GOOGLE", "provider-id-1", "a@a.com", "닉네임")));
        when(jwtTokenProvider.createAccessToken(any(), any())).thenReturn("jwt-access");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("jwt-refresh");
        when(jwtTokenProvider.refreshTokenExpiration()).thenReturn(Duration.ofDays(14));

        authService.login("google", "code", "state-value");

        verify(userRepository, never()).save(any());
        verify(loginHistoryRepository).save(any(LoginHistory.class));
    }

    @Test
    void refresh_token_rotate가_실패하면_거부된다() {
        User user = mock(User.class);
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(user.getId()).thenReturn(1L);
        when(user.getRole()).thenReturn(com.bop.youthpick.user.entity.Role.USER);
        when(jwtTokenProvider.validateRefreshToken("refresh-token")).thenReturn(claims);
        when(jwtTokenProvider.getUserId(claims)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(1L, "USER")).thenReturn("new-access");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh");
        when(jwtTokenProvider.refreshTokenExpiration()).thenReturn(Duration.ofDays(14));
        when(refreshTokenStore.rotate(1L, "refresh-token", "new-refresh", Duration.ofDays(14)))
                .thenReturn(false);

        assertThatThrownBy(() -> authService.refresh("refresh-token"))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void refresh_token이_유효하면_원자적으로_rotate하고_토큰을_재발급한다() {
        User user = mock(User.class);
        io.jsonwebtoken.Claims claims = mock(io.jsonwebtoken.Claims.class);
        when(user.getId()).thenReturn(1L);
        when(user.getRole()).thenReturn(com.bop.youthpick.user.entity.Role.USER);
        when(jwtTokenProvider.validateRefreshToken("refresh-token")).thenReturn(claims);
        when(jwtTokenProvider.getUserId(claims)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(1L, "USER")).thenReturn("new-access");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("new-refresh");
        when(jwtTokenProvider.refreshTokenExpiration()).thenReturn(Duration.ofDays(14));
        when(jwtTokenProvider.accessTokenExpirationSeconds()).thenReturn(1800L);
        when(refreshTokenStore.rotate(1L, "refresh-token", "new-refresh", Duration.ofDays(14)))
                .thenReturn(true);

        TokenResponse tokens = authService.refresh("refresh-token");

        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        verify(refreshTokenStore).rotate(1L, "refresh-token", "new-refresh", Duration.ofDays(14));
    }

    @Test
    void 동시_콜백으로_유니크_제약이_위반되면_재조회로_복구한다() {
        when(oAuthStateStore.consume("state-value", "GOOGLE")).thenReturn(true);
        when(oAuthClient.exchangeCodeForAccessToken(
                        eq(OAuthProvider.GOOGLE),
                        eq("client-id"),
                        eq("client-secret"),
                        anyString(),
                        eq("code")))
                .thenReturn("provider-access-token");
        when(oAuthClient.fetchUserInfo(OAuthProvider.GOOGLE, "provider-access-token"))
                .thenReturn(new OAuthUserInfo("GOOGLE", "provider-id-1", "a@a.com", "닉네임"));
        User existingUser = User.createSocialUser("GOOGLE", "provider-id-1", "a@a.com", "닉네임");
        when(userRepository.findByProviderAndProviderId("GOOGLE", "provider-id-1"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class)))
                .thenThrow(
                        new org.springframework.dao.DataIntegrityViolationException(
                                "uk_users_provider"));
        when(jwtTokenProvider.createAccessToken(any(), any())).thenReturn("jwt-access");
        when(jwtTokenProvider.createRefreshToken(any())).thenReturn("jwt-refresh");
        when(jwtTokenProvider.refreshTokenExpiration()).thenReturn(Duration.ofDays(14));
        when(jwtTokenProvider.accessTokenExpirationSeconds()).thenReturn(1800L);

        TokenResponse tokens = authService.login("google", "code", "state-value");

        assertThat(tokens.accessToken()).isEqualTo("jwt-access");
        verify(userRepository, org.mockito.Mockito.times(2))
                .findByProviderAndProviderId("GOOGLE", "provider-id-1");
        verify(loginHistoryRepository).save(any(LoginHistory.class));
    }

    @Test
    void 로그아웃하면_refresh_token을_삭제한다() {
        authService.logout(1L);

        verify(refreshTokenStore).delete(1L);
    }

    private Map<String, String> parseQuery(String url) {
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
