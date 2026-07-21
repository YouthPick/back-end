package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyComparisonItemResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정책 비교. 비교 결과를 저장하지 않고 요청받은 정책들을 그때그때 조회해 내려주는 순수 조회다 — 응답은 항상 조회 시점의 최신 정책 정보다. 개수 제약(2~3개)은
 * 컨트롤러의 Bean Validation이 검증하고, 여기서는 "서로 다른 정책"이라는 의미적 제약만 본다.
 */
@Service
@RequiredArgsConstructor
public class PolicyComparisonService {

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;

    /**
     * 요청 순서를 그대로 보존해 비교 대상 정책을 조회한다(비교표 열 순서 = 사용자가 고른 순서). policyIds에 중복이 있으면 비교가 성립하지 않으므로
     * INVALID_COMPARISON_REQUEST, 존재하지 않는 정책이 섞여 있으면 POLICY_NOT_FOUND를 던진다.
     */
    @Transactional(readOnly = true)
    public List<PolicyComparisonItemResponse> compare(List<Long> policyIds) {
        if (policyIds.stream().distinct().count() != policyIds.size()) {
            throw new CustomException(PolicyErrorCode.INVALID_COMPARISON_REQUEST);
        }

        List<Policy> policies = policyRepository.findAllById(policyIds);
        if (policies.size() != policyIds.size()) {
            throw new CustomException(PolicyErrorCode.POLICY_NOT_FOUND);
        }

        Map<Long, Policy> policyById =
                policies.stream().collect(Collectors.toMap(Policy::getId, Function.identity()));
        Map<Long, List<RegionResponse>> regionsByPolicyId = findRegionsByPolicyId(policyIds);
        return policyIds.stream()
                .map(
                        policyId ->
                                PolicyComparisonItemResponse.from(
                                        policyById.get(policyId),
                                        regionsByPolicyId.getOrDefault(policyId, List.of())))
                .toList();
    }

    /** 지역은 전국 정책이면 정책당 최대 256행이라, fetch join 배치 조회로 한 번에 가져와 N+1을 피한다. */
    private Map<Long, List<RegionResponse>> findRegionsByPolicyId(List<Long> policyIds) {
        return policyRegionRepository.findWithRegionByPolicyIdIn(policyIds).stream()
                .collect(
                        Collectors.groupingBy(
                                policyRegion -> policyRegion.getPolicy().getId(),
                                Collectors.mapping(
                                        policyRegion ->
                                                RegionResponse.from(policyRegion.getRegion()),
                                        Collectors.toList())));
    }
}
