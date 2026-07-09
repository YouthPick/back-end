package com.bop.youthpick.auth.service;

import com.bop.youthpick.auth.client.OAuthClient;
import com.bop.youthpick.auth.client.OAuthProvider;
import com.bop.youthpick.auth.config.OAuthProperties;
import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.dto.OAuthUserInfo;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

/** 카카오/구글/네이버 OAuth2 authorization code 로그인 흐름. 세션은 Spring Session(Redis)에 저장된다. */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String STATE_SESSION_KEY = "OAUTH_STATE";

    private final OAuthProperties oAuthProperties;
    private final OAuthClient oAuthClient;
    private final UserRepository userRepository;
    private final SecurityContextRepository securityContextRepository;

    public String buildAuthorizationUrl(String registrationId, HttpSession session) {
        OAuthProvider provider = OAuthProvider.from(registrationId);
        OAuthProperties.Registration registration = registration(provider);

        String state = UUID.randomUUID().toString();
        session.setAttribute(STATE_SESSION_KEY, state);

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(provider.getAuthorizationUri())
                        .queryParam("response_type", "code")
                        .queryParam("client_id", registration.clientId())
                        .queryParam("redirect_uri", redirectUri(provider))
                        .queryParam("state", state);
        if (StringUtils.hasText(provider.getScope())) {
            builder.queryParam("scope", provider.getScope());
        }
        return builder.encode().build().toUriString();
    }

    @Transactional
    public void login(
            String registrationId,
            String code,
            String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        OAuthProvider provider = OAuthProvider.from(registrationId);
        validateState(request, state);

        OAuthProperties.Registration registration = registration(provider);
        String accessToken =
                oAuthClient.exchangeCodeForAccessToken(
                        provider,
                        registration.clientId(),
                        registration.clientSecret(),
                        redirectUri(provider),
                        code);
        OAuthUserInfo userInfo = oAuthClient.fetchUserInfo(provider, accessToken);

        User user =
                userRepository
                        .findByProviderAndProviderId(provider.name(), userInfo.providerId())
                        .orElseGet(
                                () ->
                                        userRepository.save(
                                                User.createSocialUser(
                                                        provider.name(),
                                                        userInfo.providerId(),
                                                        userInfo.email(),
                                                        userInfo.nickname())));

        establishSession(user, request, response);
    }

    @Transactional(readOnly = true)
    public User getCurrentUser(Long userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNAUTHORIZED));
    }

    public String frontendRedirectUri() {
        return oAuthProperties.frontendRedirectUri();
    }

    private void validateState(HttpServletRequest request, String state) {
        HttpSession session = request.getSession(false);
        Object savedState = session == null ? null : session.getAttribute(STATE_SESSION_KEY);
        if (savedState == null || !savedState.equals(state)) {
            throw new AuthException(AuthErrorCode.INVALID_OAUTH_STATE);
        }
        session.removeAttribute(STATE_SESSION_KEY);
    }

    private void establishSession(
            User user, HttpServletRequest request, HttpServletResponse response) {
        AuthPrincipal principal = new AuthPrincipal(user.getId(), user.getRole().name());
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 세션 고정(fixation) 공격 방지: 로그인 성공 시 세션 id를 교체한다.
        request.changeSessionId();
        securityContextRepository.saveContext(context, request, response);
    }

    private OAuthProperties.Registration registration(OAuthProvider provider) {
        Optional<OAuthProperties.Registration> registration =
                Optional.ofNullable(oAuthProperties.providers())
                        .map(providers -> providers.get(provider.registrationId()));
        return registration
                .filter(r -> StringUtils.hasText(r.clientId()))
                .orElseThrow(() -> new AuthException(AuthErrorCode.UNSUPPORTED_OAUTH_PROVIDER));
    }

    private String redirectUri(OAuthProvider provider) {
        return oAuthProperties.redirectBaseUri()
                + "/api/v1/auth/oauth/"
                + provider.registrationId()
                + "/callback";
    }
}
