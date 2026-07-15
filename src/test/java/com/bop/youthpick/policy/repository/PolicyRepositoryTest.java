package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PolicyRepositoryTest {

    @Autowired private PolicyRepository policyRepository;

    @Test
    void visibility별_soft_delete되지_않은_정책만_카운트한다() {
        policyRepository.save(newPolicy("P001", PolicyVisibility.VISIBLE, null));
        policyRepository.save(newPolicy("P002", PolicyVisibility.VISIBLE, null));
        policyRepository.save(
                newPolicy("P003", PolicyVisibility.VISIBLE, LocalDateTime.now())); // soft delete
        policyRepository.save(newPolicy("P004", PolicyVisibility.HIDDEN, null));

        assertThat(policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.VISIBLE))
                .isEqualTo(2);
        assertThat(policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.HIDDEN))
                .isEqualTo(1);
    }

    private Policy newPolicy(
            String policyNo, PolicyVisibility visibility, LocalDateTime deletedAt) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "visibility", visibility);
        ReflectionTestUtils.setField(policy, "deletedAt", deletedAt);
        return policy;
    }
}
