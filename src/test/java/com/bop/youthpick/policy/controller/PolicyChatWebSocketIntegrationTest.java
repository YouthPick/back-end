package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.bop.youthpick.auth.service.JwtTokenProvider;
import com.bop.youthpick.policy.dto.PolicyChatErrorResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessageCreateRequest;
import com.bop.youthpick.policy.dto.PolicyChatMessageResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.repository.PolicyChatMessageRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.service.PolicyChatSubscriptionRegistry.SubscriptionType;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PolicyChatWebSocketIntegrationTest {

    @LocalServerPort private int port;

    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PolicyChatMessageRepository messageRepository;
    @Autowired private PolicyChatSubscriptionRegistry subscriptionRegistry;
    @Autowired private SimpUserRegistry simpUserRegistry;
    @Autowired private ObjectMapper objectMapper;
    @MockitoSpyBean private SimpMessagingTemplate messagingTemplate;

    private WebSocketStompClient stompClient;
    private final List<StompSession> sessions = new ArrayList<>();

    @BeforeEach
    void setUp() {
        stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        stompClient.setMessageConverter(converter);
    }

    @AfterEach
    void tearDown() {
        sessions.forEach(session -> subscriptionRegistry.disconnect(session.getSessionId()));
        sessions.stream().filter(StompSession::isConnected).forEach(StompSession::disconnect);
        stompClient.stop();
    }

    @Test
    void CONNECT_JWT_SUBSCRIBE_SEND가_커밋된_메시지를_수신자별_mine으로_전달한다() throws Exception {
        Policy policy = policyRepository.save(newPolicy("CHAT-STOMP-1"));
        User author =
                userRepository.save(
                        User.createSocialUser("kakao", "chat-stomp-author", null, "작성자"));
        User reader =
                userRepository.save(
                        User.createSocialUser("kakao", "chat-stomp-reader", null, "독자"));
        StompSession authorSession = connect(author.getId());
        StompSession readerSession = connect(reader.getId());
        CompletableFuture<PolicyChatMessageResponse> authorMessage = new CompletableFuture<>();
        CompletableFuture<PolicyChatMessageResponse> readerMessage = new CompletableFuture<>();
        CompletableFuture<PolicyChatErrorResponse> sendError = new CompletableFuture<>();
        String userDestination = "/user/queue/policies/" + policy.getId() + "/chat/messages";
        subscribe(authorSession, userDestination, PolicyChatMessageResponse.class, authorMessage);
        subscribe(readerSession, userDestination, PolicyChatMessageResponse.class, readerMessage);
        subscribe(
                authorSession,
                "/user/queue/policies/" + policy.getId() + "/chat/errors",
                PolicyChatErrorResponse.class,
                sendError);
        awaitSubscriptions(policy.getId(), SubscriptionType.MESSAGES, 2L);
        awaitSubscriptions(policy.getId(), SubscriptionType.ERRORS, 1L);
        awaitBrokerSubscription(author.getId(), userDestination);
        awaitBrokerSubscription(reader.getId(), userDestination);

        authorSession.send(
                "/app/policies/" + policy.getId() + "/chat/messages",
                new PolicyChatMessageCreateRequest("STOMP 메시지", "stomp-client-message"));

        ArgumentCaptor<String> recipients = ArgumentCaptor.forClass(String.class);
        verify(messagingTemplate, timeout(5_000).times(2))
                .convertAndSendToUser(
                        recipients.capture(),
                        org.mockito.ArgumentMatchers.eq(
                                "/queue/policies/" + policy.getId() + "/chat/messages"),
                        org.mockito.ArgumentMatchers.any());
        assertThat(recipients.getAllValues())
                .containsExactlyInAnyOrder(author.getId().toString(), reader.getId().toString());
        CompletableFuture.anyOf(authorMessage, sendError).get(5, TimeUnit.SECONDS);
        assertThat(sendError.isDone())
                .withFailMessage(
                        () ->
                                "SEND failed with "
                                        + sendError.join().code()
                                        + ": "
                                        + sendError.join().message())
                .isFalse();
        PolicyChatMessageResponse authorView = authorMessage.join();
        PolicyChatMessageResponse readerView = readerMessage.get(5, TimeUnit.SECONDS);
        assertThat(authorView.id()).isEqualTo(readerView.id());
        assertThat(authorView.policyId()).isEqualTo(policy.getId());
        assertThat(authorView.content()).isEqualTo("STOMP 메시지");
        assertThat(authorView.mine()).isTrue();
        assertThat(authorView.clientMessageId()).isEqualTo("stomp-client-message");
        assertThat(readerView.mine()).isFalse();
        assertThat(readerView.clientMessageId()).isNull();
        assertThat(messageRepository.findById(authorView.id())).isPresent();
    }

    @Test
    void 길이_제한을_초과한_clientMessageId는_C001을_보내고_저장하지_않는다() throws Exception {
        Policy policy = policyRepository.save(newPolicy("CHAT-STOMP-CLIENT-ID-ERROR"));
        User user =
                userRepository.save(
                        User.createSocialUser(
                                "kakao", "chat-stomp-client-id-error", null, "오류 사용자"));
        StompSession session = connect(user.getId());
        CompletableFuture<PolicyChatErrorResponse> error = new CompletableFuture<>();
        subscribe(
                session,
                "/user/queue/policies/" + policy.getId() + "/chat/errors",
                PolicyChatErrorResponse.class,
                error);
        awaitSubscriptions(policy.getId(), SubscriptionType.ERRORS, 1L);
        awaitBrokerSubscription(
                user.getId(), "/user/queue/policies/" + policy.getId() + "/chat/errors");

        session.send(
                "/app/policies/" + policy.getId() + "/chat/messages",
                new PolicyChatMessageCreateRequest("메시지", "x".repeat(101)));

        assertThat(error.get(5, TimeUnit.SECONDS).code()).isEqualTo("C001");
        assertThat(
                        messageRepository
                                .findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                                        policy.getId(), 0L))
                .isEmpty();
    }

    @Test
    void 빈_SEND는_저장하지_않고_정책별_error_queue에_C001을_보낸다() throws Exception {
        Policy policy = policyRepository.save(newPolicy("CHAT-STOMP-ERROR-1"));
        User user =
                userRepository.save(
                        User.createSocialUser("kakao", "chat-stomp-error-user", null, "오류 사용자"));
        StompSession session = connect(user.getId());
        CompletableFuture<PolicyChatErrorResponse> error = new CompletableFuture<>();
        subscribe(
                session,
                "/user/queue/policies/" + policy.getId() + "/chat/errors",
                PolicyChatErrorResponse.class,
                error);
        awaitSubscriptions(policy.getId(), SubscriptionType.ERRORS, 1L);
        awaitBrokerSubscription(
                user.getId(), "/user/queue/policies/" + policy.getId() + "/chat/errors");

        session.send(
                "/app/policies/" + policy.getId() + "/chat/messages",
                new PolicyChatMessageCreateRequest("   ", null));

        assertThat(error.get(5, TimeUnit.SECONDS).code()).isEqualTo("C001");
        assertThat(
                        messageRepository
                                .findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                                        policy.getId(), 0L))
                .isEmpty();
    }

    private StompSession connect(Long userId) throws Exception {
        StompHeaders headers = new StompHeaders();
        headers.add(
                HttpHeaders.AUTHORIZATION,
                "Bearer " + jwtTokenProvider.createAccessToken(userId, "USER"));
        StompSession session =
                stompClient
                        .connectAsync(
                                webSocketUrl(),
                                new WebSocketHttpHeaders(),
                                headers,
                                new StompSessionHandlerAdapter() {})
                        .get(5, TimeUnit.SECONDS);
        sessions.add(session);
        return session;
    }

    private <T> void subscribe(
            StompSession session,
            String destination,
            Class<T> payloadType,
            CompletableFuture<T> payload)
            throws Exception {
        session.subscribe(
                destination,
                new StompFrameHandler() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return payloadType;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object framePayload) {
                        payload.complete(payloadType.cast(framePayload));
                    }
                });
    }

    private void awaitSubscriptions(Long policyId, SubscriptionType type, long expected)
            throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            if (subscriptionRegistry.registrationCount(policyId, type) == expected) {
                return;
            }
            Thread.sleep(10L);
        }
        assertThat(subscriptionRegistry.registrationCount(policyId, type)).isEqualTo(expected);
    }

    private void awaitBrokerSubscription(Long userId, String destination) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (System.nanoTime() < deadline) {
            var user = simpUserRegistry.getUser(userId.toString());
            if (user != null
                    && user.getSessions().stream()
                            .flatMap(session -> session.getSubscriptions().stream())
                            .anyMatch(
                                    subscription ->
                                            destination.equals(subscription.getDestination()))) {
                return;
            }
            Thread.sleep(10L);
        }
        assertThat(simpUserRegistry.getUser(userId.toString())).isNotNull();
    }

    private String webSocketUrl() {
        return "ws://localhost:" + port + "/api/ws";
    }

    private Policy newPolicy(String policyNo) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", "STOMP 테스트 정책");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        return policy;
    }
}
