package com.bop.youthpick.policy.service;

import com.bop.youthpick.policy.entity.PolicyChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
public class PolicyChatStompPublisher {

    private final PolicyChatSubscriptionRegistry subscriptionRegistry;
    private final SimpMessagingTemplate messagingTemplate;
    private final TaskExecutor taskExecutor;

    public void publishAfterCommit(PolicyChatMessage savedMessage, String clientMessageId) {
        publishAfterCommit(PolicyChatOutboundMessage.from(savedMessage, clientMessageId));
    }

    void publishAfterCommit(PolicyChatOutboundMessage outboundMessage) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        taskExecutor.execute(() -> publish(outboundMessage));
                    }
                });
    }

    void publish(PolicyChatOutboundMessage message) {
        String destination = "/queue/policies/" + message.policyId() + "/chat/messages";
        subscriptionRegistry.messageSubscribers(message.policyId()).stream()
                .forEach(
                        subscriber ->
                                messagingTemplate.convertAndSendToUser(
                                        subscriber.userName(),
                                        destination,
                                        message.forUser(subscriber.userId())));
    }
}
