package com.bop.youthpick.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthenticationFilterTest {

    private final JwtTokenProvider jwtTokenProvider = mock(JwtTokenProvider.class);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtTokenProvider);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_토큰이면_인증정보를_채운다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.getUserId("valid-token")).thenReturn(1L);
        when(jwtTokenProvider.getRole("valid-token")).thenReturn("USER");

        filter.doFilter(request, response, filterChain);

        AuthPrincipal principal =
                (AuthPrincipal)
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo("USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_없으면_인증정보를_채우지_않는다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void 토큰이_유효하지_않으면_인증정보를_비우고_통과시킨다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
        doThrowInvalidToken();

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private void doThrowInvalidToken() {
        org.mockito.Mockito.doThrow(new AuthException(AuthErrorCode.INVALID_TOKEN))
                .when(jwtTokenProvider)
                .validateAccessToken("invalid-token");
    }
}
