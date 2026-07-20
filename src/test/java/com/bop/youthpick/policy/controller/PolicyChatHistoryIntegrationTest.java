package com.bop.youthpick.policy.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.service.JwtTokenProvider;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyChatMessage;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.repository.PolicyChatMessageRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PolicyChatHistoryIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PolicyChatMessageRepository messageRepository;

    @Test
    void 인증된_GET은_wait없이_cursor_이후_정책_메시지만_반환한다() throws Exception {
        Policy policy = policyRepository.save(newPolicy("CHAT-HISTORY-1"));
        User user =
                userRepository.save(
                        User.createSocialUser("kakao", "chat-history-user", null, "이력 사용자"));
        PolicyChatMessage first =
                messageRepository.save(PolicyChatMessage.create(policy, user, "첫 메시지"));
        PolicyChatMessage second =
                messageRepository.save(PolicyChatMessage.create(policy, user, "둘째 메시지"));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), "USER");

        mockMvc.perform(
                        get("/api/v1/policies/{policyId}/chat/messages", policy.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                                .param("afterId", first.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages.length()").value(1))
                .andExpect(jsonPath("$.data.messages[0].id").value(second.getId()))
                .andExpect(jsonPath("$.data.messages[0].policyId").value(policy.getId()))
                .andExpect(jsonPath("$.data.messages[0].mine").value(true))
                .andExpect(jsonPath("$.data.messages[0].email").doesNotExist())
                .andExpect(jsonPath("$.data.messages[0].provider").doesNotExist())
                .andExpect(jsonPath("$.data.messages[0].userId").doesNotExist())
                .andExpect(jsonPath("$.data.messages[0].token").doesNotExist())
                .andExpect(jsonPath("$.data.messages[0].clientMessageId").doesNotExist())
                .andExpect(jsonPath("$.data.nextCursor").value(second.getId()));
    }

    @Test
    void 존재하지_않는_JWT_사용자의_GET은_USER_NOT_FOUND를_반환한다() throws Exception {
        Policy policy = policyRepository.save(newPolicy("CHAT-HISTORY-MISSING-USER"));
        String accessToken = jwtTokenProvider.createAccessToken(Long.MAX_VALUE, "USER");

        mockMvc.perform(
                        get("/api/v1/policies/{policyId}/chat/messages", policy.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"));
    }

    @Test
    void 삭제된_JWT_사용자의_GET은_USER_NOT_FOUND를_반환한다() throws Exception {
        Policy policy = policyRepository.save(newPolicy("CHAT-HISTORY-DELETED-USER"));
        User deletedUser =
                userRepository.save(
                        User.createSocialUser(
                                "kakao", "chat-history-deleted-user", null, "삭제 사용자"));
        String accessToken = jwtTokenProvider.createAccessToken(deletedUser.getId(), "USER");
        deletedUser.softDelete();
        userRepository.saveAndFlush(deletedUser);

        mockMvc.perform(
                        get("/api/v1/policies/{policyId}/chat/messages", policy.getId())
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("U001"));
    }

    private Policy newPolicy(String policyNo) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", "이력 테스트 정책");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        return policy;
    }
}
