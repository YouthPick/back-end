package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.policy.dto.PolicyRecentViewResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRecentView;
import com.bop.youthpick.policy.repository.PolicyRecentViewRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyRecentViewServiceTest {

    @Mock private PolicyRecentViewRepository policyRecentViewRepository;
    @Mock private UserRepository userRepository;

    private PolicyRecentViewService policyRecentViewService;

    private final User user = User.createSocialUser("google", "pid", null, null);
    private final Policy policy = newPolicy(10L, "청년 월세 지원");

    @BeforeEach
    void setUp() {
        policyRecentViewService =
                new PolicyRecentViewService(policyRecentViewRepository, userRepository);
    }

    @Test
    void 처음_본_정책이면_새_기록을_저장한다() {
        when(policyRecentViewRepository.findByUserIdAndPolicyId(1L, 10L))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(user);

        policyRecentViewService.record(1L, policy);

        ArgumentCaptor<PolicyRecentView> captor = ArgumentCaptor.forClass(PolicyRecentView.class);
        verify(policyRecentViewRepository).save(captor.capture());
        assertThat(captor.getValue().getPolicy()).isEqualTo(policy);
        assertThat(captor.getValue().getViewedAt()).isNotNull();
    }

    @Test
    void 이미_본_정책이면_새_기록_없이_viewedAt만_갱신한다() {
        PolicyRecentView existing = PolicyRecentView.create(user, policy);
        LocalDateTime oldViewedAt = LocalDateTime.now().minusDays(3);
        ReflectionTestUtils.setField(existing, "viewedAt", oldViewedAt);
        when(policyRecentViewRepository.findByUserIdAndPolicyId(1L, 10L))
                .thenReturn(Optional.of(existing));

        policyRecentViewService.record(1L, policy);

        verify(policyRecentViewRepository, never()).save(any());
        assertThat(existing.getViewedAt()).isAfter(oldViewedAt);
    }

    @Test
    void 최근_본_목록을_카드_응답으로_변환한다() {
        PolicyRecentView view = PolicyRecentView.create(user, policy);
        Page<PolicyRecentView> page = new PageImpl<>(List.of(view));
        when(policyRecentViewRepository.findVisibleByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<PolicyRecentViewResponse> result =
                policyRecentViewService.getRecentPolicies(1L, PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).policyId()).isEqualTo(10L);
        assertThat(result.getContent().get(0).title()).isEqualTo("청년 월세 지원");
    }

    private Policy newPolicy(Long id, String title) {
        Policy newPolicy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(newPolicy, "id", id);
        ReflectionTestUtils.setField(newPolicy, "policyNo", "R2026" + id);
        ReflectionTestUtils.setField(newPolicy, "title", title);
        return newPolicy;
    }
}
