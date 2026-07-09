package com.bop.youthpick.auth.dto;

import java.io.Serializable;

/** 세션(Redis)에 저장되는 인증 principal. Redis 직렬화 대상이므로 최소 필드만 담는다. */
public record AuthPrincipal(Long userId, String role) implements Serializable {}
