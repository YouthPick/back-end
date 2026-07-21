package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.bop.youthpick.policy.dto.PolicyChatMessageResponse;
import com.bop.youthpick.policy.service.PolicyChatSubscriptionRegistry.SubscriptionType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class PolicyChatStompPublisherTest {

    @Mock private SimpMessagingTemplate messagingTemplate;

    private PolicyChatSubscriptionRegistry registry;
    private PolicyChatStompPublisher publisher;

    @BeforeEach
    void setUp() {
        registry = new PolicyChatSubscriptionRegistry();
        publisher =
                new PolicyChatStompPublisher(registry, messagingTemplate, new SyncTaskExecutor());
    }

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void 같은_정책의_활성_사용자에게만_mine과_작성자_correlation으로_전송한다() throws Exception {
        registry.register("s1", "m1", 1L, "1", 10L, SubscriptionType.MESSAGES);
        registry.register("s2", "m2", 2L, "2", 10L, SubscriptionType.MESSAGES);
        registry.register("s3", "m3", 3L, "3", 20L, SubscriptionType.MESSAGES);
        registry.register("s1", "e1", 1L, "1", 10L, SubscriptionType.ERRORS);
        PolicyChatOutboundMessage message = message();

        publisher.publish(message);

        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> destination = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2))
                .convertAndSendToUser(user.capture(), destination.capture(), payload.capture());
        assertThat(user.getAllValues()).containsExactlyInAnyOrder("1", "2");
        assertThat(destination.getAllValues()).containsOnly("/queue/policies/10/chat/messages");
        assertThat(payload.getAllValues())
                .allSatisfy(
                        value -> assertThat(value).isInstanceOf(PolicyChatMessageResponse.class));
        PolicyChatMessageResponse authorView = responseFor("1", user, payload);
        PolicyChatMessageResponse otherView = responseFor("2", user, payload);
        assertThat(authorView.mine()).isTrue();
        assertThat(otherView.mine()).isFalse();
        assertThat(authorView.clientMessageId()).isEqualTo("client-message-1");
        assertThat(otherView.clientMessageId()).isNull();
        assertThat(authorView)
                .hasNoNullFieldsOrProperties()
                .extracting(
                        PolicyChatMessageResponse::policyId,
                        PolicyChatMessageResponse::authorName,
                        PolicyChatMessageResponse::content)
                .containsExactly(10L, "작성자", "메시지");
        JsonMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        JsonNode authorJson = objectMapper.valueToTree(authorView);
        JsonNode otherJson = objectMapper.valueToTree(otherView);
        assertThat(authorJson.path("clientMessageId").asText()).isEqualTo("client-message-1");
        assertThat(otherJson.has("clientMessageId")).isFalse();
    }

    @Test
    void 트랜잭션_커밋_후에만_STOMP로_전송한다() {
        registry.register("s1", "m1", 1L, "1", 10L, SubscriptionType.MESSAGES);
        TransactionSynchronizationManager.initSynchronization();
        publisher.publishAfterCommit(message());

        verify(messagingTemplate, never()).convertAndSendToUser(any(), any(), any());
        TransactionSynchronizationManager.getSynchronizations().stream()
                .forEach(TransactionSynchronization::afterCommit);
        verify(messagingTemplate)
                .convertAndSendToUser(
                        "1",
                        "/queue/policies/10/chat/messages",
                        new PolicyChatMessageResponse(
                                11L,
                                10L,
                                "작성자",
                                "메시지",
                                LocalDateTime.of(2026, 7, 19, 12, 0),
                                true,
                                "client-message-1"));
    }

    private PolicyChatMessageResponse responseFor(
            String expectedUser, ArgumentCaptor<String> users, ArgumentCaptor<Object> payloads) {
        int index = users.getAllValues().indexOf(expectedUser);
        return (PolicyChatMessageResponse) payloads.getAllValues().get(index);
    }

    private PolicyChatOutboundMessage message() {
        return new PolicyChatOutboundMessage(
                11L,
                10L,
                1L,
                "작성자",
                "메시지",
                LocalDateTime.of(2026, 7, 19, 12, 0),
                "client-message-1");
    }
}
