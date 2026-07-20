package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import io.jsonwebtoken.Claims;
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
    void 비동기_재디스패치에서도_JWT_인증을_수행한다() {
        assertThat(filter.shouldNotFilterAsyncDispatch()).isFalse();
    }

    @Test
    void 유효한_토큰이면_인증정보를_채운다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.validateAccessToken("valid-token")).thenReturn(claims);
        when(jwtTokenProvider.getUserId(claims)).thenReturn(1L);
        when(jwtTokenProvider.getRole(claims)).thenReturn("USER");

        filter.doFilter(request, response, filterChain);

        AuthPrincipal principal =
                (AuthPrincipal)
                        SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo(1L);
        assertThat(principal.role()).isEqualTo("USER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void role이_없으면_인증정보를_채우지_않고_통과시킨다() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain filterChain = mock(FilterChain.class);
        Claims claims = mock(Claims.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
        when(jwtTokenProvider.validateAccessToken("valid-token")).thenReturn(claims);
        when(jwtTokenProvider.getUserId(claims)).thenReturn(1L);
        when(jwtTokenProvider.getRole(claims)).thenReturn(null);

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
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
        doThrow(new AuthException(AuthErrorCode.INVALID_TOKEN))
                .when(jwtTokenProvider)
                .validateAccessToken("invalid-token");

        filter.doFilter(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
