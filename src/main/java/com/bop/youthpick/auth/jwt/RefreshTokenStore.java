package com.bop.youthpick.auth.jwt;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** 사용자별 refresh token을 Redis에 TTL과 함께 저장한다. 로그인/재발급 시 덮어쓰고, 로그아웃 시 삭제한다. */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh-token:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId), refreshToken, ttl);
    }

    public Optional<String> find(Long userId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(key(userId)));
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
