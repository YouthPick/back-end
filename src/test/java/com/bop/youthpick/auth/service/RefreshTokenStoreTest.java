package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class RefreshTokenStoreTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private RefreshTokenStore refreshTokenStore;

    @BeforeEach
    void setUp() {
        refreshTokenStore = new RefreshTokenStore(redisTemplate);
    }

    @Test
    void 저장하면_원문이_아니라_해시값을_TTL과_함께_저장한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);

        refreshTokenStore.save(1L, "refresh-token", Duration.ofDays(14));

        verify(valueOperations)
                .set(eq("auth:refresh-token:1"), valueCaptor.capture(), eq(Duration.ofDays(14)));
        assertThat(valueCaptor.getValue()).isNotEqualTo("refresh-token");
        assertThat(valueCaptor.getValue()).hasSize(64); // SHA-256 hex 문자열 길이
    }

    @Test
    void 저장된_해시와_일치하면_matches가_true를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        refreshTokenStore.save(1L, "refresh-token", Duration.ofDays(14));
        verify(valueOperations).set(eq("auth:refresh-token:1"), hashCaptor.capture(), any());
        when(valueOperations.get("auth:refresh-token:1")).thenReturn(hashCaptor.getValue());

        assertThat(refreshTokenStore.matches(1L, "refresh-token")).isTrue();
    }

    @Test
    void 저장된_값과_불일치하면_matches가_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh-token:1")).thenReturn("different-hash");

        assertThat(refreshTokenStore.matches(1L, "refresh-token")).isFalse();
    }

    @Test
    void 저장된_값이_없으면_matches가_false를_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh-token:1")).thenReturn(null);

        assertThat(refreshTokenStore.matches(1L, "refresh-token")).isFalse();
    }

    @Test
    void 삭제하면_userId_키를_지운다() {
        refreshTokenStore.delete(1L);

        verify(redisTemplate).delete("auth:refresh-token:1");
    }
}
