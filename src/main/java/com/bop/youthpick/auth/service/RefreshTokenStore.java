package com.bop.youthpick.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 사용자별 refresh token을 Redis에 TTL과 함께 저장한다. Redis가 유출되더라도 원문 토큰이 그대로 노출되지 않도록 SHA-256 해시만 저장하고,
 * 재발급(rotate)은 Lua 스크립트로 "저장값 일치 확인 + 교체"를 원자적으로 처리해 동시 요청으로 인한 중복 발급을 막는다.
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "auth:refresh-token:";

    private static final RedisScript<Long> ROTATE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    local stored = redis.call('GET', KEYS[1])
                    if stored == ARGV[1] then
                        redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
                        return 1
                    else
                        return 0
                    end
                    """,
                    Long.class);

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        redisTemplate.opsForValue().set(key(userId), hash(refreshToken), ttl);
    }

    /** 저장된 값이 {@code oldRefreshToken}의 해시와 일치할 때만 {@code newRefreshToken}으로 원자적으로 교체한다. */
    public boolean rotate(
            Long userId, String oldRefreshToken, String newRefreshToken, Duration ttl) {
        Long result =
                redisTemplate.execute(
                        ROTATE_SCRIPT,
                        List.of(key(userId)),
                        hash(oldRefreshToken),
                        hash(newRefreshToken),
                        String.valueOf(ttl.toMillis()));
        return result != null && result == 1L;
    }

    public void delete(Long userId) {
        redisTemplate.delete(key(userId));
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다.", e);
        }
    }
}
