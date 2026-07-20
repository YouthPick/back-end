package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyChatMessage;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PolicyChatMessageRepositoryTest {

    @Autowired private PolicyRepository policyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private PolicyChatMessageRepository messageRepository;

    @Test
    void 정책과_exclusive_cursor로_메시지를_ID_오름차순_조회한다() {
        Policy firstPolicy = policyRepository.save(newPolicy("P001"));
        Policy secondPolicy = policyRepository.save(newPolicy("P002"));
        User user =
                userRepository.save(
                        User.createSocialUser("kakao", "provider-1", "private@example.com", "청년"));
        PolicyChatMessage first =
                messageRepository.saveAndFlush(
                        PolicyChatMessage.create(firstPolicy, user, "첫 메시지"));
        PolicyChatMessage second =
                messageRepository.saveAndFlush(
                        PolicyChatMessage.create(firstPolicy, user, "둘째 메시지"));
        messageRepository.saveAndFlush(PolicyChatMessage.create(secondPolicy, user, "다른 정책"));
        PolicyChatMessage deleted =
                messageRepository.saveAndFlush(
                        PolicyChatMessage.create(firstPolicy, user, "삭제 메시지"));
        ReflectionTestUtils.setField(deleted, "deletedAt", LocalDateTime.now());
        messageRepository.saveAndFlush(deleted);

        List<PolicyChatMessage> result =
                messageRepository.findByPolicyIdAndIdGreaterThanAndDeletedAtIsNullOrderByIdAsc(
                        firstPolicy.getId(), first.getId(), Pageable.ofSize(50));

        assertThat(result).extracting(PolicyChatMessage::getId).containsExactly(second.getId());
        assertThat(result)
                .allMatch(message -> message.getPolicy().getId().equals(firstPolicy.getId()));
    }

    private Policy newPolicy(String policyNo) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        return policy;
    }
}
