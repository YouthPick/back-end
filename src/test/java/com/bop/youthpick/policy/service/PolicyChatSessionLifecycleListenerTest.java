package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.policy.service.PolicyChatSubscriptionRegistry.SubscriptionType;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

class PolicyChatSessionLifecycleListenerTest {

    @Test
    void transport가_갑자기_종료되어도_session_등록을_모두_정리한다() {
        PolicyChatSubscriptionRegistry registry = new PolicyChatSubscriptionRegistry();
        registry.register("session-1", "sub-1", 1L, "1", 10L, SubscriptionType.MESSAGES);
        registry.register("session-1", "sub-2", 1L, "1", 10L, SubscriptionType.ERRORS);
        SessionDisconnectEvent event = mock(SessionDisconnectEvent.class);
        when(event.getSessionId()).thenReturn("session-1");

        new PolicyChatSessionLifecycleListener(registry).handleDisconnect(event);

        assertThat(registry.registrationCount()).isZero();
    }
}
