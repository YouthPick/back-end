package com.bop.youthpick.auth.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.dto.TokenResponse;
import com.bop.youthpick.auth.service.AuthService;
import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
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
    void 콜백_성공시_토큰을_JSON으로_반환한다() throws Exception {
        when(authService.login("google", "auth-code", "state-value"))
                .thenReturn(TokenResponse.of("access-token", "refresh-token", 1800));

        mockMvc.perform(
                        post("/api/v1/auth/oauth/{provider}/callback", "google")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"code": "auth-code", "state": "state-value"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
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
    void refresh_토큰으로_재발급하면_새_토큰을_반환한다() throws Exception {
        when(authService.refresh("old-refresh-token"))
                .thenReturn(TokenResponse.of("new-access", "new-refresh", 1800));

        mockMvc.perform(
                        post("/api/v1/auth/token/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"refreshToken": "old-refresh-token"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh"));
    }

    @Test
    void 인증된_사용자가_로그아웃하면_204를_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(post("/api/v1/auth/logout"))
                                .andExpect(status().isNoContent()));
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
