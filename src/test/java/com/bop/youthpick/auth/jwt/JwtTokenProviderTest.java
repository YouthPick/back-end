package com.bop.youthpick.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bop.youthpick.auth.config.JwtProperties;
import com.bop.youthpick.auth.exception.AuthException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties =
                new JwtProperties(
                        "test-secret-key-please-be-long-enough-for-hs256",
                        Duration.ofMinutes(30),
                        Duration.ofDays(14));
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);
    }

    @Test
    void access_token을_생성하고_검증하면_userId와_role을_읽을_수_있다() {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        jwtTokenProvider.validateAccessToken(token);
        assertThat(jwtTokenProvider.getUserId(token)).isEqualTo(1L);
        assertThat(jwtTokenProvider.getRole(token)).isEqualTo("USER");
    }

    @Test
    void refresh_token을_access_token으로_검증하면_예외() {
        String refreshToken = jwtTokenProvider.createRefreshToken(1L);

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(refreshToken))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void access_token을_refresh_token으로_검증하면_예외() {
        String accessToken = jwtTokenProvider.createAccessToken(1L, "USER");

        assertThatThrownBy(() -> jwtTokenProvider.validateRefreshToken(accessToken))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void 위조된_토큰은_검증에_실패한다() {
        String token = jwtTokenProvider.createAccessToken(1L, "USER") + "tampered";

        assertThatThrownBy(() -> jwtTokenProvider.validateAccessToken(token))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void 만료된_토큰은_검증에_실패한다() {
        JwtProperties expiredProperties =
                new JwtProperties(
                        "test-secret-key-please-be-long-enough-for-hs256",
                        Duration.ofMillis(1),
                        Duration.ofDays(14));
        JwtTokenProvider expiredTokenProvider = new JwtTokenProvider(expiredProperties);
        String token = expiredTokenProvider.createAccessToken(1L, "USER");

        await();

        assertThatThrownBy(() -> expiredTokenProvider.validateAccessToken(token))
                .isInstanceOf(AuthException.class);
    }

    private void await() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
