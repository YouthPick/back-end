package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.sync.dto.PolicySyncJobSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminPolicySyncJobServiceTest {

    @Mock private PolicyRepository policyRepository;

    private AdminPolicySyncJobService adminPolicySyncJobService;

    @BeforeEach
    void setUp() {
        adminPolicySyncJobService = new AdminPolicySyncJobService(policyRepository);
    }

    @Test
    void 요약_조회는_visibility별_카운트를_담고_배치_파이프라인_미구현_필드는_0이다() {
        when(policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.VISIBLE))
                .thenReturn(3241L);
        when(policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.HIDDEN))
                .thenReturn(12L);

        PolicySyncJobSummaryResponse summary = adminPolicySyncJobService.getSummary();

        assertThat(summary.activeCount()).isEqualTo(3241L);
        assertThat(summary.missingCount()).isEqualTo(12L);
        assertThat(summary.parseErrorCount()).isZero();
        assertThat(summary.dbFailCount()).isZero();
    }
}
