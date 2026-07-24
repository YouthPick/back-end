package com.bop.youthpick.post.service;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/** Redis를 활용하여 동일 유저/IP의 게시글 중복 조회를 방지하는 Store */
@Component
@RequiredArgsConstructor
public class PostViewLogStore {

    private final StringRedisTemplate redisTemplate;

    private static final String KEY_PREFIX = "post:view:";
    private static final Duration LOCK_DURATION = Duration.ofDays(1); // 24시간 중복 방지

    /**
     * 해당 게시글에 대해 주어진 식별자가 처음으로 조회하는지 여부를 검증하고, 처음이라면 24시간 동안 유효한 중복 방지 락 키를 생성한다.
     *
     * @param postId 게시글 ID
     * @param identifier 식별자 (user:{userId} 또는 ip:{ipAddress})
     * @return 첫 조회일 경우 true, 이미 조회한 이력이 있는 경우 false
     */
    public boolean isFirstView(Long postId, String identifier) {
        String key = KEY_PREFIX + postId + ":" + identifier;
        Boolean isFirst = redisTemplate.opsForValue().setIfAbsent(key, "Y", LOCK_DURATION);
        return Boolean.TRUE.equals(isFirst);
    }
}
