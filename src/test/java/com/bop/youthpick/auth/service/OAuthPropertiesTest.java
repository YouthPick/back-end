package com.bop.youthpick.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OAuthPropertiesTest {

    @Test
    void Registration_toString은_clientSecret을_마스킹한다() {
        OAuthProperties.Registration registration =
                new OAuthProperties.Registration("client-id", "super-secret-value");

        String result = registration.toString();

        assertThat(result).contains("client-id");
        assertThat(result).doesNotContain("super-secret-value");
    }
}
