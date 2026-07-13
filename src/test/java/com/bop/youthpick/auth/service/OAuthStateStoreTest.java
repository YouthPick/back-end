package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class OAuthStateStoreTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private OAuthStateStore oAuthStateStore;

    @BeforeEach
    void setUp() {
        oAuthStateStore = new OAuthStateStore(redisTemplate);
    }

    @Test
    void 저장된_provider와_일치하면_소비하고_true를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:oauth-state:state-1")).thenReturn("GOOGLE");

        boolean consumed = oAuthStateStore.consume("state-1", "GOOGLE");

        assertThat(consumed).isTrue();
        verify(redisTemplate).delete("auth:oauth-state:state-1");
    }

    @Test
    void provider가_다르면_소비하지_않고_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:oauth-state:state-1")).thenReturn("KAKAO");

        boolean consumed = oAuthStateStore.consume("state-1", "GOOGLE");

        assertThat(consumed).isFalse();
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    void state가_없으면_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:oauth-state:unknown")).thenReturn(null);

        boolean consumed = oAuthStateStore.consume("unknown", "GOOGLE");

        assertThat(consumed).isFalse();
    }
}
