package com.bop.youthpick.policy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class PolicyChatSessionLifecycleListener {

    private final PolicyChatSubscriptionRegistry subscriptionRegistry;

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        subscriptionRegistry.disconnect(event.getSessionId());
    }
}
