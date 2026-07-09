package com.bop.youthpick.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthException;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class CurrentUserArgumentResolverTest {

    private final CurrentUserArgumentResolver resolver = new CurrentUserArgumentResolver();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void CurrentUser가_붙은_Long_파라미터를_지원한다() throws Exception {
        MethodParameter parameter = annotatedUserIdParameter();

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    void CurrentUser가_없으면_지원하지_않는다() throws Exception {
        MethodParameter parameter = plainUserIdParameter();

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    void 인증된_요청이면_principal의_userId를_그대로_반환한다() throws Exception {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(1L, "USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        Object resolved = resolver.resolveArgument(annotatedUserIdParameter(), null, null, null);

        assertThat(resolved).isEqualTo(1L);
    }

    @Test
    void 인증되지_않았으면_예외() throws Exception {
        assertThatThrownBy(
                        () ->
                                resolver.resolveArgument(
                                        annotatedUserIdParameter(), null, null, null))
                .isInstanceOf(AuthException.class);
    }

    private MethodParameter annotatedUserIdParameter() throws Exception {
        Method method = TestController.class.getMethod("withCurrentUser", Long.class);
        return new MethodParameter(method, 0);
    }

    private MethodParameter plainUserIdParameter() throws Exception {
        Method method = TestController.class.getMethod("withoutCurrentUser", Long.class);
        return new MethodParameter(method, 0);
    }

    private static class TestController {
        public void withCurrentUser(@CurrentUser Long userId) {}

        public void withoutCurrentUser(Long userId) {}
    }
}
