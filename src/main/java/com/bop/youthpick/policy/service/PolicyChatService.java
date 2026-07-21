package com.bop.youthpick.policy.service;

import com.bop.youthpick.policy.dto.PolicyChatMessageCreateRequest;
import com.bop.youthpick.policy.dto.PolicyChatMessageResponse;
import com.bop.youthpick.policy.dto.PolicyChatMessagesResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyChatMessage;
import com.bop.youthpick.policy.repository.PolicyChatMessageRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyChatService {

    private static final int MESSAGE_PAGE_SIZE = 50;

    private final PolicyChatAccessService accessService;
    private final UserRepository userRepository;
    private final PolicyChatMessageRepository messageRepository;
    private final PolicyChatStompPublisher stompPublisher;

    @Transactional(readOnly = true)
    public PolicyChatMessagesResponse getMessages(Long policyId, Long userId, Long afterId) {
        requireActiveUser(userId);
        accessService.requireVisiblePolicy(policyId);
        List<PolicyChatMessageResponse> messages =
                messageRepository
                        .findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                                policyId, afterId, Pageable.ofSize(MESSAGE_PAGE_SIZE))
                        .stream()
                        .map(message -> PolicyChatMessageResponse.from(message, userId))
                        .toList();
        return PolicyChatMessagesResponse.of(messages, afterId);
    }

    @Transactional
    public PolicyChatMessageResponse send(
            Long policyId, Long userId, PolicyChatMessageCreateRequest request) {
        Policy policy = accessService.requireVisiblePolicy(policyId);
        User user = requireActiveUser(userId);
        PolicyChatMessage message =
                messageRepository.save(PolicyChatMessage.create(policy, user, request.content()));
        stompPublisher.publishAfterCommit(message, request.clientMessageId());
        return PolicyChatMessageResponse.from(message, userId);
    }

    private User requireActiveUser(Long userId) {
        return userRepository
                .findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new UserException(UserError.USER_NOT_FOUND));
    }
}
