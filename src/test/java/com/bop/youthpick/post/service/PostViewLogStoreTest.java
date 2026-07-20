package com.bop.youthpick.post.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class PostViewLogStoreTest {

    @Mock private StringRedisTemplate redisTemplate;

    @Mock private ValueOperations<String, String> valueOperations;

    private PostViewLogStore postViewLogStore;

    @BeforeEach
    void setUp() {
        postViewLogStore = new PostViewLogStore(redisTemplate);
    }

    @Test
    void 처음_조회하면_true를_반환하고_락을_설정한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("post:view:1:user:1"), eq("Y"), any(Duration.class)))
                .thenReturn(true);

        boolean result = postViewLogStore.isFirstView(1L, "user:1");

        assertThat(result).isTrue();
        verify(valueOperations).setIfAbsent(eq("post:view:1:user:1"), eq("Y"), any(Duration.class));
    }

    @Test
    void 이미_조회했으면_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(eq("post:view:1:user:1"), eq("Y"), any(Duration.class)))
                .thenReturn(false);

        boolean result = postViewLogStore.isFirstView(1L, "user:1");

        assertThat(result).isFalse();
    }
}
