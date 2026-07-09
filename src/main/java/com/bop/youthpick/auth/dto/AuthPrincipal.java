package com.bop.youthpick.auth.dto;

/** JWT access token에서 추출한 인증 principal. {@code JwtAuthenticationFilter}가 요청마다 채운다. */
public record AuthPrincipal(Long userId, String role) {}
