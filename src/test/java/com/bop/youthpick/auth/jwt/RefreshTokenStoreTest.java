package com.bop.youthpick.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    void 저장하면_TTL과_함께_userId_키로_저장한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        refreshTokenStore.save(1L, "refresh-token", Duration.ofDays(14));

        verify(valueOperations).set("auth:refresh-token:1", "refresh-token", Duration.ofDays(14));
    }

    @Test
    void 저장된_값이_있으면_조회된다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh-token:1")).thenReturn("refresh-token");

        Optional<String> found = refreshTokenStore.find(1L);

        assertThat(found).contains("refresh-token");
    }

    @Test
    void 저장된_값이_없으면_빈_값을_반환한다() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("auth:refresh-token:1")).thenReturn(null);

        Optional<String> found = refreshTokenStore.find(1L);

        assertThat(found).isEmpty();
    }

    @Test
    void 삭제하면_userId_키를_지운다() {
        refreshTokenStore.delete(1L);

        verify(redisTemplate).delete("auth:refresh-token:1");
    }
}
