package com.bop.youthpick.auth.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * OAuth authorization-url 발급 시점에 CSRF 방지용 state를 Redis에 짧은 TTL로 저장하고, 콜백에서 1회 검증 후 소비한다. 세션을 쓰지 않는
 * stateless 인증이라 서버 쪽 임시 상태는 여기 Redis에 둔다.
 */
@Component
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final String KEY_PREFIX = "auth:oauth-state:";
    private static final Duration TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;

    public void save(String state, String provider) {
        redisTemplate.opsForValue().set(key(state), provider, TTL);
    }

    /** state에 저장된 provider와 일치하면 소비(삭제)하고 true를 반환한다. */
    public boolean consume(String state, String provider) {
        String key = key(state);
        String savedProvider = redisTemplate.opsForValue().get(key);
        if (savedProvider == null || !savedProvider.equals(provider)) {
            return false;
        }
        redisTemplate.delete(key);
        return true;
    }

    private String key(String state) {
        return KEY_PREFIX + state;
    }
}
