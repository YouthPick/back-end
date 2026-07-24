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

    /**
     * state를 원자적으로 소비(GETDEL)하고, 저장된 provider와 일치하면 true를 반환한다. GET 후 DELETE 2단계로 나누면 동시 요청 2건이 모두
     * 통과할 수 있어 getAndDelete로 1회 소비를 보장한다.
     */
    public boolean consume(String state, String provider) {
        String savedProvider = redisTemplate.opsForValue().getAndDelete(key(state));
        return savedProvider != null && savedProvider.equals(provider);
    }

    private String key(String state) {
        return KEY_PREFIX + state;
    }
}
