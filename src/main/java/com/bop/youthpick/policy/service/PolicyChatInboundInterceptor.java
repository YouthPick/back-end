package com.bop.youthpick.policy.service;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.auth.service.JwtStompAuthentication;
import com.bop.youthpick.auth.service.JwtTokenProvider;
import com.bop.youthpick.policy.service.PolicyChatSubscriptionRegistry.SubscriptionType;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import java.security.Principal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class PolicyChatInboundInterceptor implements ChannelInterceptor {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final Pattern SEND_DESTINATION =
            Pattern.compile("^/app/policies/([1-9]\\d*)/chat/messages$");
    private static final Pattern SUBSCRIBE_DESTINATION =
            Pattern.compile("^/user/queue/policies/([1-9]\\d*)/chat/(messages|errors)$");

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PolicyChatAccessService accessService;
    private final PolicyChatSubscriptionRegistry subscriptionRegistry;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SEND -> authorizeSend(accessor);
            case SUBSCRIBE -> authorizeSubscribe(accessor);
            default -> {
                return message;
            }
        }
        return message;
    }

    @Override
    public void afterSendCompletion(
            Message<?> message, MessageChannel channel, boolean sent, Exception exception) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return;
        }
        if (accessor.getCommand() == StompCommand.DISCONNECT) {
            disconnect(accessor);
            return;
        }
        if (!sent || exception != null) {
            return;
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            register(accessor);
        } else if (accessor.getCommand() == StompCommand.UNSUBSCRIBE) {
            unregister(accessor);
        }
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization) || !authorization.startsWith(BEARER_PREFIX)) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }
        String token = authorization.substring(BEARER_PREFIX.length());
        if (!StringUtils.hasText(token)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        Claims claims = jwtTokenProvider.validateAccessToken(token);
        Long userId = jwtTokenProvider.getUserId(claims);
        String role = jwtTokenProvider.getRole(claims);
        if (!StringUtils.hasText(role)) {
            throw new AuthException(AuthErrorCode.INVALID_TOKEN);
        }
        userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
        accessor.setUser(new JwtStompAuthentication(userId, role));
    }

    private void authorizeSend(StompHeaderAccessor accessor) {
        requireAuthentication(accessor);
        Long policyId = requirePolicyId(accessor.getDestination(), SEND_DESTINATION);
        accessService.requireVisiblePolicy(policyId);
    }

    private void authorizeSubscribe(StompHeaderAccessor accessor) {
        requireAuthentication(accessor);
        requireText(accessor.getSessionId());
        requireText(accessor.getSubscriptionId());
        Long policyId = requirePolicyId(accessor.getDestination(), SUBSCRIBE_DESTINATION);
        accessService.requireVisiblePolicy(policyId);
    }

    private void register(StompHeaderAccessor accessor) {
        Authentication authentication = requireAuthentication(accessor);
        Matcher matcher = SUBSCRIBE_DESTINATION.matcher(accessor.getDestination());
        if (!matcher.matches()) {
            return;
        }
        AuthPrincipal principal = (AuthPrincipal) authentication.getPrincipal();
        SubscriptionType type =
                "messages".equals(matcher.group(2))
                        ? SubscriptionType.MESSAGES
                        : SubscriptionType.ERRORS;
        subscriptionRegistry.register(
                accessor.getSessionId(),
                accessor.getSubscriptionId(),
                principal.userId(),
                authentication.getName(),
                Long.valueOf(matcher.group(1)),
                type);
    }

    private void unregister(StompHeaderAccessor accessor) {
        if (StringUtils.hasText(accessor.getSessionId())
                && StringUtils.hasText(accessor.getSubscriptionId())) {
            subscriptionRegistry.unregister(accessor.getSessionId(), accessor.getSubscriptionId());
        }
    }

    private void disconnect(StompHeaderAccessor accessor) {
        if (StringUtils.hasText(accessor.getSessionId())) {
            subscriptionRegistry.disconnect(accessor.getSessionId());
        }
    }

    private Authentication requireAuthentication(StompHeaderAccessor accessor) {
        Principal user = accessor.getUser();
        if (!(user instanceof Authentication authentication)
                || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthPrincipal)) {
            throw new AuthException(AuthErrorCode.UNAUTHORIZED);
        }
        return authentication;
    }

    private Long requirePolicyId(String destination, Pattern pattern) {
        if (!StringUtils.hasText(destination)) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
        Matcher matcher = pattern.matcher(destination);
        if (!matcher.matches()) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
        try {
            return Long.valueOf(matcher.group(1));
        } catch (NumberFormatException exception) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
    }

    private void requireText(String value) {
        if (!StringUtils.hasText(value)) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
    }
}
