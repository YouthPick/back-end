package com.bop.youthpick.auth.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.dto.TokenResponse;
import com.bop.youthpick.auth.service.AuthService;
import com.bop.youthpick.auth.service.RefreshTokenCookieSupport;
import com.bop.youthpick.global.config.CorsProperties;
import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.entity.User;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuthService authService;

    @MockitoBean private RefreshTokenCookieSupport refreshTokenCookieSupport;

    @MockitoBean private CorsProperties corsProperties;

    @BeforeEach
    void setUp() {
        when(corsProperties.allowedOrigins()).thenReturn(List.of("http://localhost:5173"));
    }

    @Test
    void 인가_url_요청시_ApiResponse로_url을_반환한다() throws Exception {
        when(authService.buildAuthorizationUrl("google"))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=abc");

        mockMvc.perform(get("/api/v1/auth/oauth/{provider}/authorization-url", "google"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.authorizationUrl")
                                .value("https://accounts.google.com/o/oauth2/v2/auth?state=abc"));
    }

    @Test
    void 콜백_성공시_access_token은_JSON으로_refresh_token은_쿠키로_내려간다() throws Exception {
        when(authService.login("google", "auth-code", "state-value"))
                .thenReturn(TokenResponse.of("access-token", "refresh-token", 1800));
        stubIssuedCookie();

        mockMvc.perform(
                        post("/api/v1/auth/oauth/{provider}/callback", "google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code": "auth-code", "state": "state-value"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().value(RefreshTokenCookieSupport.COOKIE_NAME, "refresh-token"));
    }

    @Test
    void 콜백_code가_없으면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/oauth/{provider}/callback", "google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"state": "state-value"}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void refresh_쿠키가_있으면_access_token만_재발급하고_refresh_쿠키는_다시_내려가지_않는다() throws Exception {
        when(authService.refresh("old-refresh-token"))
                .thenReturn(TokenResponse.of("new-access", "old-refresh-token", 1800));

        mockMvc.perform(
                        post("/api/v1/auth/token/refresh")
                                .cookie(
                                        new Cookie(
                                                RefreshTokenCookieSupport.COOKIE_NAME,
                                                "old-refresh-token")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").doesNotExist())
                .andExpect(cookie().doesNotExist(RefreshTokenCookieSupport.COOKIE_NAME));

        verify(refreshTokenCookieSupport, never()).issue(anyString());
    }

    @Test
    void refresh_쿠키가_없으면_401과_A007을_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A007"));
    }

    @Test
    void refresh_허용되지_않은_Origin이면_403과_A008을_반환하고_서비스를_호출하지_않는다() throws Exception {
        mockMvc.perform(
                        post("/api/v1/auth/token/refresh")
                                .header("Origin", "https://evil.example.com")
                                .cookie(
                                        new Cookie(
                                                RefreshTokenCookieSupport.COOKIE_NAME,
                                                "old-refresh-token")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A008"));

        verify(authService, never()).refresh(anyString());
    }

    @Test
    void 인증된_사용자가_로그아웃하면_204와_함께_refresh_쿠키를_지운다() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(authService.getCurrentUser(1L)).thenReturn(user);
        when(refreshTokenCookieSupport.clear())
                .thenReturn(
                        ResponseCookie.from(RefreshTokenCookieSupport.COOKIE_NAME, "")
                                .path("/api/v1/auth")
                                .maxAge(Duration.ZERO)
                                .build());

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(post("/api/v1/auth/logout"))
                                .andExpect(status().isNoContent())
                                .andExpect(
                                        cookie().maxAge(RefreshTokenCookieSupport.COOKIE_NAME, 0)));
    }

    @Test
    void 인증되지_않은_me_요청은_401과_A001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void 인증된_사용자면_me_요청시_사용자_정보를_반환한다() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("a@a.com");
        when(user.getNickname()).thenReturn("닉네임");
        when(user.getRole()).thenReturn(Role.USER);
        when(authService.getCurrentUser(1L)).thenReturn(user);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(get("/api/v1/auth/me"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.id").value(1))
                                .andExpect(jsonPath("$.data.email").value("a@a.com"))
                                .andExpect(jsonPath("$.data.role").value("USER")));
    }

    private void stubIssuedCookie() {
        when(refreshTokenCookieSupport.issue(anyString()))
                .thenAnswer(
                        invocation ->
                                ResponseCookie.from(
                                                RefreshTokenCookieSupport.COOKIE_NAME,
                                                invocation.getArgument(0, String.class))
                                        .httpOnly(true)
                                        .path("/api/v1/auth")
                                        .maxAge(Duration.ofDays(14))
                                        .build());
    }

    /**
     * addFilters=false로 시큐리티 필터 체인(JwtAuthenticationFilter 포함)을 건너뛰므로, SecurityContextHolder를 직접
     * 채워 @AuthenticationPrincipal을 해석시킨다.
     */
    private void withAuthenticatedPrincipal(ThrowingRunnable runnable) throws Exception {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(1L, "USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            runnable.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
