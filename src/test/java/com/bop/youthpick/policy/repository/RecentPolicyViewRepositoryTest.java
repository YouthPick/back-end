package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.RecentPolicyView;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class RecentPolicyViewRepositoryTest {

    @Autowired private RecentPolicyViewRepository recentPolicyViewRepository;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private UserRepository userRepository;

    private User user;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.createSocialUser("google", "pid-1", null, null));
    }

    @Test
    void 삭제_숨김_정책은_제외하고_마지막_조회_시각_내림차순으로_조회한다() {
        Policy visibleOld = savePolicy("P001", PolicyVisibility.VISIBLE, null);
        Policy visibleNew = savePolicy("P002", PolicyVisibility.VISIBLE, null);
        Policy hidden = savePolicy("P003", PolicyVisibility.HIDDEN, null);
        Policy deleted = savePolicy("P004", PolicyVisibility.VISIBLE, LocalDateTime.now());

        saveView(visibleOld, LocalDateTime.now().minusDays(2));
        saveView(visibleNew, LocalDateTime.now().minusDays(1));
        saveView(hidden, LocalDateTime.now());
        saveView(deleted, LocalDateTime.now());

        Page<RecentPolicyView> page =
                recentPolicyViewRepository.findVisibleByUserId(user.getId(), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getContent())
                .extracting(view -> view.getPolicy().getPolicyNo())
                .containsExactly("P002", "P001");
    }

    @Test
    void 다른_사용자의_기록은_조회되지_않는다() {
        User other = userRepository.save(User.createSocialUser("google", "pid-2", null, null));
        Policy policy = savePolicy("P001", PolicyVisibility.VISIBLE, null);
        recentPolicyViewRepository.save(RecentPolicyView.create(other, policy));

        Page<RecentPolicyView> page =
                recentPolicyViewRepository.findVisibleByUserId(user.getId(), PageRequest.of(0, 20));

        assertThat(page.getTotalElements()).isZero();
    }

    private Policy savePolicy(
            String policyNo, PolicyVisibility visibility, LocalDateTime deletedAt) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "visibility", visibility);
        ReflectionTestUtils.setField(policy, "deletedAt", deletedAt);
        return policyRepository.save(policy);
    }

    private void saveView(Policy policy, LocalDateTime viewedAt) {
        RecentPolicyView view = RecentPolicyView.create(user, policy);
        ReflectionTestUtils.setField(view, "viewedAt", viewedAt);
        recentPolicyViewRepository.save(view);
    }
}
