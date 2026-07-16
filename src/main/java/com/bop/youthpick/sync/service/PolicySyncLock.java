package com.bop.youthpick.sync.service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * 정책 수집 배치의 중복 실행을 막는 Redis 락 (SETNX + TTL + 소유 토큰).
 *
 * <p>TTL은 락 소유자가 죽어도(OOM, kill) 락이 영원히 남지 않게 하는 안전장치이고, 소유 토큰은 TTL 만료 후 다른 실행이 잡은 락을 이전 실행이 해제하는
 * 사고를 막는다. 해제는 RefreshTokenStore와 같은 방식의 Lua 스크립트로 "소유 확인 + 삭제"를 원자 처리한다.
 */
@Component
@RequiredArgsConstructor
public class PolicySyncLock {

    static final String LOCK_KEY = "sync:policy-sync:lock";
    // 전량 수집이 수십 초 규모라 10분이면 충분히 여유 — 소유자가 죽었을 때 최대 이 시간만 잠김
    static final Duration LOCK_TTL = Duration.ofMinutes(10);

    private static final RedisScript<Long> RELEASE_SCRIPT =
            new DefaultRedisScript<>(
                    """
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    else
                        return 0
                    end
                    """,
                    Long.class);

    private final StringRedisTemplate redisTemplate;

    /** 락이 비어 있으면 획득하고 소유 토큰을 반환한다. 다른 실행이 쥐고 있으면 빈값. */
    public Optional<String> tryAcquire() {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(LOCK_KEY, token, LOCK_TTL);
        return Boolean.TRUE.equals(acquired) ? Optional.of(token) : Optional.empty();
    }

    /** 저장된 값이 자기 토큰일 때만 락을 삭제한다 — 남의 락은 건드리지 않는다. */
    public void release(String token) {
        redisTemplate.execute(RELEASE_SCRIPT, List.of(LOCK_KEY), token);
    }
}
