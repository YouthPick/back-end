package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.auth.exception.AuthException;
import com.bop.youthpick.auth.service.JwtProperties;
import com.bop.youthpick.auth.service.JwtStompAuthentication;
import com.bop.youthpick.auth.service.JwtTokenProvider;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

class PolicyChatInboundInterceptorTest {

    private JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;
    private PolicyChatAccessService accessService;
    private PolicyChatSubscriptionRegistry registry;
    private PolicyChatInboundInterceptor interceptor;
    private MessageChannel channel;

    @BeforeEach
    void setUp() {
        jwtTokenProvider =
                new JwtTokenProvider(
                        new JwtProperties(
                                "test-secret-key-please-be-long-enough-for-hs256",
                                Duration.ofMinutes(30),
                                Duration.ofDays(14)));
        accessService = mock(PolicyChatAccessService.class);
        userRepository = mock(UserRepository.class);
        registry = new PolicyChatSubscriptionRegistry();
        interceptor =
                new PolicyChatInboundInterceptor(
                        jwtTokenProvider, userRepository, accessService, registry);
        channel = mock(MessageChannel.class);
    }

    @Test
    void CONNECT는_Bearer_access_token으로_userId와_role을_가진_Principal을_설정한다() {
        String token = jwtTokenProvider.createAccessToken(7L, "USER");
        when(userRepository.findByIdAndDeletedAtIsNull(7L))
                .thenReturn(Optional.of(mock(User.class)));
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, null, "session-1", null);
        StompHeaderAccessor accessor = accessor(connect);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        interceptor.preSend(connect, channel);

        assertThat(accessor.getUser()).isInstanceOf(JwtStompAuthentication.class);
        JwtStompAuthentication authentication = (JwtStompAuthentication) accessor.getUser();
        assertThat(authentication.getName()).isEqualTo("7");
        assertThat(authentication.getPrincipal()).isEqualTo(new AuthPrincipal(7L, "USER"));
        assertThat(authentication.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    void CONNECT는_삭제되었거나_존재하지_않는_JWT_사용자를_USER_NOT_FOUND로_거부한다() {
        String token = jwtTokenProvider.createAccessToken(99L, "USER");
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());
        Message<byte[]> connect = frame(StompCommand.CONNECT, null, null, "session-1", null);
        StompHeaderAccessor accessor = accessor(connect);
        accessor.setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        assertThatThrownBy(() -> interceptor.preSend(connect, channel))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserError.USER_NOT_FOUND);
        assertThat(accessor.getUser()).isNull();
    }

    @Test
    void CONNECT는_토큰이_없거나_유효하지_않으면_거부한다() {
        Message<byte[]> missing = frame(StompCommand.CONNECT, null, null, "session-1", null);
        Message<byte[]> invalid = frame(StompCommand.CONNECT, null, null, "session-2", null);
        accessor(invalid).setNativeHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid");

        assertThatThrownBy(() -> interceptor.preSend(missing, channel))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(() -> interceptor.preSend(invalid, channel))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void SEND는_인증과_정확한_app_목적지와_보이는_정책만_허용한다() {
        JwtStompAuthentication user = new JwtStompAuthentication(7L, "USER");
        Message<byte[]> allowed =
                frame(StompCommand.SEND, "/app/policies/10/chat/messages", user, "session-1", null);

        interceptor.preSend(allowed, channel);

        verify(accessService).requireVisiblePolicy(10L);
        assertThatThrownBy(
                        () ->
                                interceptor.preSend(
                                        frame(
                                                StompCommand.SEND,
                                                "/topic/policies/10/chat/messages",
                                                user,
                                                "session-1",
                                                null),
                                        channel))
                .isInstanceOf(AuthException.class);
        assertThatThrownBy(
                        () ->
                                interceptor.preSend(
                                        frame(
                                                StompCommand.SEND,
                                                "/app/policies/10/chat/messages",
                                                null,
                                                "session-1",
                                                null),
                                        channel))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void SUBSCRIBE는_보이지_않는_정책과_알수없는_queue를_거부한다() {
        JwtStompAuthentication user = new JwtStompAuthentication(7L, "USER");
        doThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND))
                .when(accessService)
                .requireVisiblePolicy(99L);

        assertThatThrownBy(
                        () ->
                                interceptor.preSend(
                                        frame(
                                                StompCommand.SUBSCRIBE,
                                                "/user/queue/policies/99/chat/messages",
                                                user,
                                                "session-1",
                                                "sub-1"),
                                        channel))
                .isInstanceOf(CustomException.class);
        assertThatThrownBy(
                        () ->
                                interceptor.preSend(
                                        frame(
                                                StompCommand.SUBSCRIBE,
                                                "/user/queue/global/chat/messages",
                                                user,
                                                "session-1",
                                                "sub-2"),
                                        channel))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void SUBSCRIBE_UNSUBSCRIBE_DISCONNECT가_등록을_안전하게_정리한다() {
        JwtStompAuthentication user = new JwtStompAuthentication(7L, "USER");
        Message<byte[]> first =
                frame(
                        StompCommand.SUBSCRIBE,
                        "/user/queue/policies/10/chat/messages",
                        user,
                        "session-1",
                        "sub-1");
        Message<byte[]> second =
                frame(
                        StompCommand.SUBSCRIBE,
                        "/user/queue/policies/10/chat/errors",
                        user,
                        "session-1",
                        "sub-2");
        interceptor.preSend(first, channel);
        interceptor.preSend(second, channel);
        interceptor.afterSendCompletion(first, channel, true, null);
        interceptor.afterSendCompletion(second, channel, true, null);
        assertThat(registry.registrationCount()).isEqualTo(2);

        Message<byte[]> unsubscribe =
                frame(StompCommand.UNSUBSCRIBE, null, user, "session-1", "sub-1");
        interceptor.afterSendCompletion(unsubscribe, channel, true, null);
        assertThat(registry.registrationCount()).isOne();

        Message<byte[]> disconnect = frame(StompCommand.DISCONNECT, null, user, "session-1", null);
        interceptor.afterSendCompletion(disconnect, channel, true, null);
        assertThat(registry.registrationCount()).isZero();
    }

    private Message<byte[]> frame(
            StompCommand command,
            String destination,
            JwtStompAuthentication user,
            String sessionId,
            String subscriptionId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) {
            accessor.setDestination(destination);
        }
        if (user != null) {
            accessor.setUser(user);
        }
        if (sessionId != null) {
            accessor.setSessionId(sessionId);
        }
        if (subscriptionId != null) {
            accessor.setSubscriptionId(subscriptionId);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private StompHeaderAccessor accessor(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }
}
