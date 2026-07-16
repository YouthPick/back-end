package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
class PolicySyncLockTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private PolicySyncLock policySyncLock;

    @BeforeEach
    void setUp() {
        policySyncLock = new PolicySyncLock(redisTemplate);
    }

    @Test
    void 락이_비어있으면_TTL과_함께_획득하고_소유_토큰을_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                        eq("sync:policy-sync:lock"), anyString(), eq(Duration.ofMinutes(10))))
                .thenReturn(true);

        Optional<String> token = policySyncLock.tryAcquire();

        assertThat(token).isPresent();
        assertThat(token.get()).isNotBlank();
    }

    @Test
    void 다른_실행이_락을_쥐고_있으면_빈값을_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class)))
                .thenReturn(false);

        Optional<String> token = policySyncLock.tryAcquire();

        assertThat(token).isEmpty();
    }

    @Test
    void 해제는_소유자_확인_스크립트로_자기_토큰을_전달한다() {
        policySyncLock.release("my-token");

        verify(redisTemplate)
                .execute(
                        any(RedisScript.class),
                        eq(List.of("sync:policy-sync:lock")),
                        eq("my-token"));
    }
}
