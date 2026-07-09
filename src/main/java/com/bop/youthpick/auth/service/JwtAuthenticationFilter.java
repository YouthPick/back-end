package com.bop.youthpick.auth.service;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer <accessToken>}을 읽어 인증을 채운다. 토큰이 없거나 유효하지 않으면 그냥 통과시키고, 보호된 엔드포인트라면
 * {@code RestAuthenticationEntryPoint}가 401(A001)로 응답한다.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String token = resolveToken(request);
        if (token != null) {
            try {
                // access token 파싱을 한 번만 수행하고, 검증된 Claims에서 userId/role을 함께 꺼낸다.
                Claims claims = jwtTokenProvider.validateAccessToken(token);
                Long userId = jwtTokenProvider.getUserId(claims);
                String role = jwtTokenProvider.getRole(claims);
                if (role != null) {
                    AuthPrincipal principal = new AuthPrincipal(userId, role);
                    Authentication authentication =
                            new UsernamePasswordAuthenticationToken(
                                    principal,
                                    null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (AuthException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
