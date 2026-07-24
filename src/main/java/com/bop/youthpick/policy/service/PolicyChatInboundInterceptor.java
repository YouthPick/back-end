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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
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

    /**
     * Spring의 {@code StompSubProtocolHandler}는 DISCONNECT에만 RECEIPT를 자동 응답하고 SUBSCRIBE 등 다른 커맨드는
     * 애플리케이션이 직접 echo해야 한다. 프론트({@code usePolicyChat.ts})는 SUBSCRIBE에 receipt를 걸어 구독 완료를 확인하므로, 이를
     * 수동으로 echo하지 않으면 매번 receipt 대기가 타임아웃되어 실제로는 연결이 끊기지 않았는데도 "재연결 중" 상태로 잘못 표시된다.
     *
     * <p>{@code clientOutboundChannel}을 직접(eager) 주입하면 이 인터셉터를 등록하는 {@code WebSocketConfig} →
     * {@code DelegatingWebSocketMessageBrokerConfiguration}(채널을 만드는 쪽) → 다시 {@code
     * WebSocketConfig}로 순환 참조가 생겨 기동에 실패한다. {@link ObjectProvider}로 실제 사용 시점까지 조회를 미뤄 순환을 끊는다.
     */
    @Qualifier("clientOutboundChannel")
    private final ObjectProvider<MessageChannel> clientOutboundChannelProvider;

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
        if (!sent || exception != null) {
            return;
        }
        if (accessor.getCommand() == StompCommand.SUBSCRIBE) {
            register(accessor);
            sendReceiptIfRequested(accessor);
        } else if (accessor.getCommand() == StompCommand.UNSUBSCRIBE) {
            unregister(accessor);
        }
    }

    private void sendReceiptIfRequested(StompHeaderAccessor originalAccessor) {
        String receiptId = originalAccessor.getReceipt();
        if (!StringUtils.hasText(receiptId)) {
            return;
        }
        StompHeaderAccessor receiptAccessor = StompHeaderAccessor.create(StompCommand.RECEIPT);
        receiptAccessor.setReceiptId(receiptId);
        receiptAccessor.setSessionId(originalAccessor.getSessionId());
        receiptAccessor.setLeaveMutable(true);
        Message<byte[]> receiptMessage =
                MessageBuilder.createMessage(new byte[0], receiptAccessor.getMessageHeaders());
        clientOutboundChannelProvider.getObject().send(receiptMessage);
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
