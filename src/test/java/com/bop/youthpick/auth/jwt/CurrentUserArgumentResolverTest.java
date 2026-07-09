package com.bop.youthpick.auth.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.auth.service.AuthService;
import com.bop.youthpick.user.entity.User;
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

    private final AuthService authService = mock(AuthService.class);
    private final CurrentUserArgumentResolver resolver =
            new CurrentUserArgumentResolver(authService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void CurrentUser가_붙은_User_파라미터를_지원한다() throws Exception {
        MethodParameter parameter = annotatedUserParameter();

        assertThat(resolver.supportsParameter(parameter)).isTrue();
    }

    @Test
    void CurrentUser가_없으면_지원하지_않는다() throws Exception {
        MethodParameter parameter = plainUserParameter();

        assertThat(resolver.supportsParameter(parameter)).isFalse();
    }

    @Test
    void 인증된_요청이면_userId로_User를_조회해_반환한다() throws Exception {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(1L, "USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        User user = mock(User.class);
        when(authService.getCurrentUser(1L)).thenReturn(user);

        Object resolved = resolver.resolveArgument(annotatedUserParameter(), null, null, null);

        assertThat(resolved).isSameAs(user);
    }

    @Test
    void 인증되지_않았으면_예외() throws Exception {
        assertThatThrownBy(
                        () -> resolver.resolveArgument(annotatedUserParameter(), null, null, null))
                .isInstanceOf(AuthException.class);
    }

    private MethodParameter annotatedUserParameter() throws Exception {
        Method method = TestController.class.getMethod("withCurrentUser", User.class);
        return new MethodParameter(method, 0);
    }

    private MethodParameter plainUserParameter() throws Exception {
        Method method = TestController.class.getMethod("withoutCurrentUser", User.class);
        return new MethodParameter(method, 0);
    }

    private static class TestController {
        public void withCurrentUser(@CurrentUser User user) {}

        public void withoutCurrentUser(User user) {}
    }
}
