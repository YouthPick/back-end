package com.bop.youthpick.policy.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.bop.youthpick.auth.service.JwtStompAuthentication;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyChatErrorResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessageCreateRequest;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.PolicyChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class PolicyChatMessageControllerTest {

    @Mock private PolicyChatService policyChatService;
    @Mock private SimpMessagingTemplate messagingTemplate;

    private PolicyChatMessageController controller;

    @BeforeEach
    void setUp() {
        controller = new PolicyChatMessageController(policyChatService, messagingTemplate);
    }

    @Test
    void SEND는_STOMP_Principal의_userId로_메시지를_저장하고_직접_echo하지_않는다() {
        PolicyChatMessageCreateRequest request = new PolicyChatMessageCreateRequest("메시지", null);
        JwtStompAuthentication principal = new JwtStompAuthentication(7L, "USER");

        controller.send(10L, request, principal);

        verify(policyChatService).send(10L, 7L, request);
        org.mockito.Mockito.verifyNoInteractions(messagingTemplate);
    }

    @Test
    void 비즈니스_오류는_민감정보없이_해당_정책의_user_error_queue로_보낸다() {
        JwtStompAuthentication principal = new JwtStompAuthentication(7L, "USER");

        controller.handleException(
                new CustomException(PolicyErrorCode.POLICY_NOT_FOUND),
                principal,
                "/app/policies/10/chat/messages");

        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate)
                .convertAndSendToUser(
                        org.mockito.ArgumentMatchers.eq("7"),
                        org.mockito.ArgumentMatchers.eq("/queue/policies/10/chat/errors"),
                        payload.capture());
        assertThat(payload.getValue())
                .isEqualTo(
                        new PolicyChatErrorResponse(
                                PolicyErrorCode.POLICY_NOT_FOUND.getCode(),
                                PolicyErrorCode.POLICY_NOT_FOUND.getMessage()));
    }
}
