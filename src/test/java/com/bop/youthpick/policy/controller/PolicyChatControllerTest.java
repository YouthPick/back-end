package com.bop.youthpick.policy.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.error.GlobalExceptionHandler;
import com.bop.youthpick.policy.dto.PolicyChatMessageResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessagesResponse;
import com.bop.youthpick.policy.service.PolicyChatService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class PolicyChatControllerTest {

    @Mock private PolicyChatService policyChatService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PolicyChatController controller = new PolicyChatController(policyChatService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
                        .build();
    }

    @Test
    void GET은_기본_cursor로_이력을_즉시_응답한다() throws Exception {
        PolicyChatMessagesResponse response = PolicyChatMessagesResponse.of(List.of(message()), 0L);
        when(policyChatService.getMessages(10L, 1L, 0L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/policies/{policyId}/chat/messages", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].id").value(11))
                .andExpect(jsonPath("$.data.nextCursor").value(11));
        verify(policyChatService).getMessages(10L, 1L, 0L);
    }

    @Test
    void POST_전송_엔드포인트는_존재하지_않는다() throws Exception {
        mockMvc.perform(post("/api/v1/policies/{policyId}/chat/messages", 10L))
                .andExpect(status().isMethodNotAllowed());
    }

    private PolicyChatMessageResponse message() {
        return new PolicyChatMessageResponse(
                11L, 10L, "작성자", "안녕하세요", LocalDateTime.of(2026, 7, 19, 12, 0), true, null);
    }

    private static class TestCurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return 1L;
        }
    }
}
