package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
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
    void 저장된_provider와_일치하면_원자적으로_소비하고_true를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oauth-state:state-1")).thenReturn("GOOGLE");

        boolean consumed = oAuthStateStore.consume("state-1", "GOOGLE");

        assertThat(consumed).isTrue();
        verify(valueOperations).getAndDelete("auth:oauth-state:state-1");
    }

    @Test
    void provider가_다르면_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oauth-state:state-1")).thenReturn("KAKAO");

        boolean consumed = oAuthStateStore.consume("state-1", "GOOGLE");

        assertThat(consumed).isFalse();
    }

    @Test
    void state가_없으면_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oauth-state:unknown")).thenReturn(null);

        boolean consumed = oAuthStateStore.consume("unknown", "GOOGLE");

        assertThat(consumed).isFalse();
    }

    @Test
    void 이미_소비된_state는_다시_소비할_수_없다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete("auth:oauth-state:state-1"))
                .thenReturn("GOOGLE")
                .thenReturn(null);

        assertThat(oAuthStateStore.consume("state-1", "GOOGLE")).isTrue();
        assertThat(oAuthStateStore.consume("state-1", "GOOGLE")).isFalse();
    }
}
