package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.policy.dto.RecentPolicyResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.RecentPolicyView;
import com.bop.youthpick.policy.repository.RecentPolicyViewRepository;
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
class RecentPolicyViewServiceTest {

    @Mock private RecentPolicyViewRepository recentPolicyViewRepository;
    @Mock private UserRepository userRepository;

    private RecentPolicyViewService recentPolicyViewService;

    private final User user = User.createSocialUser("google", "pid", null, null);
    private final Policy policy = newPolicy(10L, "청년 월세 지원");

    @BeforeEach
    void setUp() {
        recentPolicyViewService =
                new RecentPolicyViewService(recentPolicyViewRepository, userRepository);
    }

    @Test
    void 처음_본_정책이면_새_기록을_저장한다() {
        when(recentPolicyViewRepository.findByUserIdAndPolicyId(1L, 10L))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(recentPolicyViewRepository.countByUserId(1L)).thenReturn(1L);

        recentPolicyViewService.record(1L, policy);

        ArgumentCaptor<RecentPolicyView> captor = ArgumentCaptor.forClass(RecentPolicyView.class);
        verify(recentPolicyViewRepository).save(captor.capture());
        assertThat(captor.getValue().getPolicy()).isEqualTo(policy);
        assertThat(captor.getValue().getViewedAt()).isNotNull();
    }

    @Test
    void 이미_본_정책이면_새_기록_없이_viewedAt만_갱신한다() {
        RecentPolicyView existing = RecentPolicyView.create(user, policy);
        LocalDateTime oldViewedAt = LocalDateTime.now().minusDays(3);
        ReflectionTestUtils.setField(existing, "viewedAt", oldViewedAt);
        when(recentPolicyViewRepository.findByUserIdAndPolicyId(1L, 10L))
                .thenReturn(Optional.of(existing));
        when(recentPolicyViewRepository.countByUserId(1L)).thenReturn(1L);

        recentPolicyViewService.record(1L, policy);

        verify(recentPolicyViewRepository, never()).save(any());
        assertThat(existing.getViewedAt()).isAfter(oldViewedAt);
    }

    @Test
    void 보관_상한을_넘으면_오래된_기록부터_삭제한다() {
        when(recentPolicyViewRepository.findByUserIdAndPolicyId(1L, 10L))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        long overCount = RecentPolicyViewService.MAX_RECENT_VIEWS + 2L;
        when(recentPolicyViewRepository.countByUserId(1L)).thenReturn(overCount);
        List<RecentPolicyView> oldest =
                List.of(
                        RecentPolicyView.create(user, policy),
                        RecentPolicyView.create(user, policy));
        when(recentPolicyViewRepository.findByUserIdOrderByViewedAtAsc(1L, PageRequest.of(0, 2)))
                .thenReturn(oldest);

        recentPolicyViewService.record(1L, policy);

        verify(recentPolicyViewRepository).deleteAll(oldest);
    }

    @Test
    void 보관_상한_이내면_삭제하지_않는다() {
        when(recentPolicyViewRepository.findByUserIdAndPolicyId(1L, 10L))
                .thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(recentPolicyViewRepository.countByUserId(1L))
                .thenReturn((long) RecentPolicyViewService.MAX_RECENT_VIEWS);

        recentPolicyViewService.record(1L, policy);

        verify(recentPolicyViewRepository, never()).deleteAll(any());
    }

    @Test
    void 최근_본_목록을_카드_응답으로_변환한다() {
        RecentPolicyView view = RecentPolicyView.create(user, policy);
        Page<RecentPolicyView> page = new PageImpl<>(List.of(view));
        when(recentPolicyViewRepository.findVisibleByUserId(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        Page<RecentPolicyResponse> result =
                recentPolicyViewService.getRecentPolicies(1L, PageRequest.of(0, 20));

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
