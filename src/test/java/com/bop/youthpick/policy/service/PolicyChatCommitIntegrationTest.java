package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import com.bop.youthpick.policy.dto.PolicyChatMessageCreateRequest;
import com.bop.youthpick.policy.dto.PolicyChatMessageResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.repository.PolicyChatMessageRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.service.PolicyChatSubscriptionRegistry.SubscriptionType;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class PolicyChatCommitIntegrationTest {

    @Autowired private PolicyChatService policyChatService;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PolicyChatMessageRepository messageRepository;
    @Autowired private PolicyChatSubscriptionRegistry subscriptionRegistry;

    @MockitoBean private SimpMessagingTemplate messagingTemplate;

    @AfterEach
    void cleanUp() {
        messageRepository.deleteAll();
    }

    @Test
    void send_트랜잭션이_커밋된_뒤_저장된_메시지를_STOMP로_발행한다() {
        Policy policy = policyRepository.save(newPolicy("CHAT-COMMIT-1"));
        User user =
                userRepository.save(
                        User.createSocialUser("kakao", "chat-commit-user", null, "커밋 사용자"));
        subscriptionRegistry.register(
                "commit-session",
                "commit-subscription",
                user.getId(),
                user.getId().toString(),
                policy.getId(),
                SubscriptionType.MESSAGES);

        policyChatService.send(
                policy.getId(),
                user.getId(),
                new PolicyChatMessageCreateRequest("커밋 메시지", "commit-client-message"));

        assertThat(
                        messageRepository
                                .findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                                        policy.getId(), 0L))
                .hasSize(1);
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, timeout(1_000))
                .convertAndSendToUser(
                        org.mockito.ArgumentMatchers.eq(user.getId().toString()),
                        org.mockito.ArgumentMatchers.eq(
                                "/queue/policies/" + policy.getId() + "/chat/messages"),
                        payload.capture());
        assertThat(payload.getValue())
                .isInstanceOfSatisfying(
                        PolicyChatMessageResponse.class,
                        response -> {
                            assertThat(response.policyId()).isEqualTo(policy.getId());
                            assertThat(response.content()).isEqualTo("커밋 메시지");
                            assertThat(response.mine()).isTrue();
                            assertThat(response.clientMessageId())
                                    .isEqualTo("commit-client-message");
                        });
        subscriptionRegistry.disconnect("commit-session");
    }

    private Policy newPolicy(String policyNo) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", "커밋 테스트 정책");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        return policy;
    }
}
