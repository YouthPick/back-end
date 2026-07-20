package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PolicyChatAccessService {

    private final PolicyRepository policyRepository;

    @Transactional(readOnly = true)
    public Policy requireVisiblePolicy(Long policyId) {
        return policyRepository
                .findByIdAndVisibilityAndDeletedAtIsNull(policyId, PolicyVisibility.VISIBLE)
                .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));
    }
}
