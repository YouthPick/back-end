package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bop.youthpick.auth.exception.AuthException;
import org.junit.jupiter.api.Test;

class OAuthProviderTest {

    @Test
    void 대소문자와_무관하게_provider를_찾는다() {
        assertThat(OAuthProvider.from("google")).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(OAuthProvider.from("KAKAO")).isEqualTo(OAuthProvider.KAKAO);
    }

    @Test
    void 지원하지_않는_provider면_예외() {
        assertThatThrownBy(() -> OAuthProvider.from("facebook")).isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> OAuthProvider.from(null)).isInstanceOf(AuthException.class);
    }
}
