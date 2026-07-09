package com.bop.youthpick.auth.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.service.AuthService;
import com.bop.youthpick.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
        when(authService.buildAuthorizationUrl(eq("google"), any()))
                .thenReturn("https://accounts.google.com/o/oauth2/v2/auth?state=abc");

        mockMvc.perform(get("/api/v1/auth/oauth/{provider}/authorization-url", "google"))
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.authorizationUrl")
                                .value("https://accounts.google.com/o/oauth2/v2/auth?state=abc"));
    }

    @Test
    void 콜백_성공시_프론트엔드로_302_리다이렉트한다() throws Exception {
        when(authService.frontendRedirectUri()).thenReturn("http://localhost:3000/oauth/callback");

        mockMvc.perform(
                        get("/api/v1/auth/oauth/{provider}/callback", "google")
                                .param("code", "auth-code")
                                .param("state", "state-value"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "http://localhost:3000/oauth/callback"));
    }

    @Test
    void 로그아웃하면_204를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout")).andExpect(status().isNoContent());
    }

    @Test
    void 인증된_사용자면_me_요청시_사용자_정보를_반환한다() throws Exception {
        User user = mock(User.class);
        when(user.getId()).thenReturn(1L);
        when(user.getEmail()).thenReturn("a@a.com");
        when(user.getNickname()).thenReturn("닉네임");
        when(user.getRole()).thenReturn(com.bop.youthpick.user.entity.Role.USER);
        when(authService.getCurrentUser(1L)).thenReturn(user);

        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(1L, "USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        // addFilters=false로 시큐리티 필터 체인을 건너뛰므로, authentication() 포스트프로세서가 세션에 남기는
        // 값은 실제로 읽히지 않는다. SecurityContextHolder를 직접 채워 @AuthenticationPrincipal을 해석시킨다.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            mockMvc.perform(get("/api/v1/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.email").value("a@a.com"))
                    .andExpect(jsonPath("$.data.role").value("USER"));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
