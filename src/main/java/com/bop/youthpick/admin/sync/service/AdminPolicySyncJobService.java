package com.bop.youthpick.admin.sync.service;

import com.bop.youthpick.admin.sync.dto.PolicySyncJobSummaryResponse;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPolicySyncJobService {

    private final PolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public PolicySyncJobSummaryResponse getSummary() {
        long activeCount =
                policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.VISIBLE);
        long missingCount =
                policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.HIDDEN);
        return PolicySyncJobSummaryResponse.of(activeCount, missingCount);
    }
}
