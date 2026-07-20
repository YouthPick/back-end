package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyChatMessageCreateRequest;
import com.bop.youthpick.policy.dto.PolicyChatMessageResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessagesResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyChatMessage;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyChatMessageRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyChatServiceTest {

    @Mock private PolicyChatAccessService accessService;
    @Mock private UserRepository userRepository;
    @Mock private PolicyChatMessageRepository messageRepository;
    @Mock private PolicyChatStompPublisher stompPublisher;

    private PolicyChatService policyChatService;

    @BeforeEach
    void setUp() {
        policyChatService =
                new PolicyChatService(
                        accessService, userRepository, messageRepository, stompPublisher);
    }

    @Test
    void 보이는_정책의_메시지만_cursor_이후로_조회한다() {
        Policy policy = newPolicy(10L, PolicyVisibility.VISIBLE, null);
        User mine = newUser(1L, "내 닉네임", "private@example.com");
        User other = newUser(2L, null, "other@example.com");
        PolicyChatMessage first = newMessage(11L, policy, mine, "첫 메시지");
        PolicyChatMessage second = newMessage(12L, policy, other, "둘째 메시지");
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(mine));
        when(accessService.requireVisiblePolicy(10L)).thenReturn(policy);
        when(messageRepository.findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                        10L, 7L))
                .thenReturn(List.of(first, second));

        PolicyChatMessagesResponse response = policyChatService.getMessages(10L, 1L, 7L);

        assertThat(response.nextCursor()).isEqualTo(12L);
        assertThat(response.messages())
                .extracting(PolicyChatMessageResponse::id)
                .containsExactly(11L, 12L);
        assertThat(response.messages().get(0).mine()).isTrue();
        assertThat(response.messages().get(1).authorName()).isEqualTo("사용자");
    }

    @Test
    void 삭제되었거나_존재하지_않는_사용자는_메시지_이력을_조회할_수_없다() {
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyChatService.getMessages(10L, 99L, 0L))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserError.USER_NOT_FOUND);

        verify(messageRepository, never())
                .findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(any(), any());
    }

    @Test
    void 숨김_정책이면_POLICY_NOT_FOUND를_던지고_메시지를_조회하지_않는다() {
        when(userRepository.findByIdAndDeletedAtIsNull(1L))
                .thenReturn(Optional.of(newUser(1L, "사용자", null)));
        when(accessService.requireVisiblePolicy(10L))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        assertPolicyNotFound(() -> policyChatService.getMessages(10L, 1L, 0L));

        verify(messageRepository, never())
                .findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(any(), any());
    }

    @Test
    void 삭제_정책이면_POLICY_NOT_FOUND를_던지고_메시지를_보내지_않는다() {
        when(accessService.requireVisiblePolicy(10L))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        assertPolicyNotFound(
                () ->
                        policyChatService.send(
                                10L, 1L, new PolicyChatMessageCreateRequest("메시지", null)));

        verify(messageRepository, never()).save(any());
        verify(stompPublisher, never()).publishAfterCommit(any(PolicyChatMessage.class), any());
    }

    @Test
    void 삭제되었거나_존재하지_않는_사용자는_메시지를_보낼_수_없다() {
        Policy policy = newPolicy(10L, PolicyVisibility.VISIBLE, null);
        when(accessService.requireVisiblePolicy(10L)).thenReturn(policy);
        when(userRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(
                        () ->
                                policyChatService.send(
                                        10L, 99L, new PolicyChatMessageCreateRequest("메시지", null)))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserError.USER_NOT_FOUND);

        verify(messageRepository, never()).save(any());
        verify(stompPublisher, never()).publishAfterCommit(any(PolicyChatMessage.class), any());
    }

    @Test
    void 메시지를_저장하고_정책별_커밋후_알림을_예약한다() {
        Policy policy = newPolicy(10L, PolicyVisibility.VISIBLE, null);
        User user = newUser(1L, "청년", "private@example.com");
        PolicyChatMessage saved = newMessage(20L, policy, user, "안녕하세요");
        when(accessService.requireVisiblePolicy(10L)).thenReturn(policy);
        when(userRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(user));
        when(messageRepository.save(any(PolicyChatMessage.class))).thenReturn(saved);

        PolicyChatMessageResponse response =
                policyChatService.send(
                        10L, 1L, new PolicyChatMessageCreateRequest("안녕하세요", "client-message-1"));

        assertThat(response.id()).isEqualTo(20L);
        assertThat(response.policyId()).isEqualTo(10L);
        assertThat(response.authorName()).isEqualTo("청년");
        assertThat(response.mine()).isTrue();
        verify(stompPublisher).publishAfterCommit(saved, "client-message-1");
    }

    private void assertPolicyNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.POLICY_NOT_FOUND);
    }

    private Policy newPolicy(Long id, PolicyVisibility visibility, LocalDateTime deletedAt) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "id", id);
        ReflectionTestUtils.setField(policy, "policyNo", "P" + id);
        ReflectionTestUtils.setField(policy, "title", "정책 " + id);
        ReflectionTestUtils.setField(policy, "visibility", visibility);
        ReflectionTestUtils.setField(policy, "deletedAt", deletedAt);
        return policy;
    }

    private User newUser(Long id, String nickname, String email) {
        User user = User.createSocialUser("kakao", "provider-" + id, email, nickname);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private PolicyChatMessage newMessage(Long id, Policy policy, User user, String content) {
        PolicyChatMessage message = PolicyChatMessage.create(policy, user, content);
        ReflectionTestUtils.setField(message, "id", id);
        ReflectionTestUtils.setField(message, "createdAt", LocalDateTime.of(2026, 7, 19, 12, 0));
        return message;
    }
}
