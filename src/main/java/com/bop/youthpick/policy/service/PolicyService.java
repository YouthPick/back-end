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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 대상 정책 조회. 관리자용 조회/수정은 {@link AdminPolicyService}. */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final PolicyRecentViewService policyRecentViewService;

    /**
     * 정책 상세 조회. 삭제(soft delete)·숨김 정책은 존재하지 않는 것으로 취급한다. 로그인 사용자({@code userId != null})의 조회는 최근 본
     * 정책으로 기록하되, 기록은 부가 동작이라 실패해도 조회는 정상 응답한다.
     */
    @Transactional(readOnly = true)
    public PolicyDetailResponse getDetail(Long policyId, @Nullable Long userId) {
        Policy policy =
                policyRepository
                        .findByIdAndVisibilityAndDeletedAtIsNull(policyId, PolicyVisibility.VISIBLE)
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        if (userId != null) {
            try {
                policyRecentViewService.record(userId, policy);
            } catch (RuntimeException e) {
                log.warn(
                        "최근 본 정책 기록에 실패했습니다. 조회는 정상 진행합니다. userId={}, policyId={}",
                        userId,
                        policyId,
                        e);
            }
        }

        List<String> regionCodes =
                policyRegionRepository.findByPolicyIdIn(List.of(policyId)).stream()
                        .map(policyRegion -> policyRegion.getRegion().getCode())
                        .toList();
        return PolicyDetailResponse.from(policy, regionCodes);
    }
}
