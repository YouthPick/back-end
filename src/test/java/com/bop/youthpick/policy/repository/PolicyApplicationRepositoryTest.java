package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.service.PolicyApplicationService;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import({JpaAuditingConfig.class, PolicyApplicationService.class})
class PolicyApplicationRepositoryTest {

    @Autowired private PolicyApplicationRepository policyApplicationRepository;
    @Autowired private PolicyApplicationService policyApplicationService;
    @Autowired private UserRepository userRepository;
    @Autowired private TestEntityManager entityManager;

    @Test
    void soft_delete된_신청을_SQLRestriction과_무관하게_재등록용으로_조회한다() {
        User user = userRepository.save(User.createSocialUser("kakao", "kakao-1", null, "닉네임"));
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", "P001");
        ReflectionTestUtils.setField(policy, "title", "청년 일자리 지원");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        entityManager.persistAndFlush(policy);

        PolicyApplication application =
                PolicyApplication.create(user, policy, ApplicationStatus.INTERESTED, null, null);
        entityManager.persistAndFlush(application);
        application.delete();
        entityManager.flush();
        entityManager.clear();

        Optional<PolicyApplication> found =
                policyApplicationRepository.findIncludingDeletedByUserIdAndPolicyId(
                        user.getId(), policy.getId());

        assertThat(found).isPresent();
        assertThat(found.orElseThrow().isDeleted()).isTrue();
    }

    @Test
    void 관심_해제후_같은_정책을_재등록하면_기존_행을_재활성화한다() {
        User user = userRepository.save(User.createSocialUser("kakao", "kakao-2", null, "닉네임"));
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", "P002");
        ReflectionTestUtils.setField(policy, "title", "청년 주거 지원");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        entityManager.persistAndFlush(policy);

        PolicyApplication created =
                policyApplicationService.create(
                        user.getId(), policy.getId(), ApplicationStatus.INTERESTED, null, null);
        Long applicationId = created.getId();
        policyApplicationService.delete(applicationId, user.getId());
        entityManager.flush();
        entityManager.clear();

        PolicyApplication reactivated =
                policyApplicationService.create(
                        user.getId(), policy.getId(), ApplicationStatus.INTERESTED, null, null);

        assertThat(reactivated.getId()).isEqualTo(applicationId);
        assertThat(reactivated.isDeleted()).isFalse();
        assertThat(reactivated.getStatus()).isEqualTo(ApplicationStatus.INTERESTED);
    }
}
