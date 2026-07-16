package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 대상 정책 조회. 관리자용 조회/수정은 {@link AdminPolicyService}. */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final RecentPolicyViewService recentPolicyViewService;

    /**
     * 정책 상세 조회. 삭제(soft delete)·숨김 정책은 존재하지 않는 것으로 취급한다. 로그인 사용자({@code userId != null})의 조회는 최근 본
     * 정책으로 기록한다.
     */
    @Transactional
    public PolicyDetailResponse getDetail(Long policyId, @Nullable Long userId) {
        Policy policy =
                policyRepository
                        .findByIdAndVisibilityAndDeletedAtIsNull(policyId, PolicyVisibility.VISIBLE)
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        if (userId != null) {
            recentPolicyViewService.record(userId, policy);
        }

        List<String> regionCodes =
                policyRegionRepository.findByPolicyIdIn(List.of(policyId)).stream()
                        .map(policyRegion -> policyRegion.getRegion().getCode())
                        .toList();
        return PolicyDetailResponse.from(policy, regionCodes);
    }
}
