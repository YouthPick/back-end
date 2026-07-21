package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyComparisonCreateRequest;
import com.bop.youthpick.policy.dto.PolicyComparisonItemResponse;
import com.bop.youthpick.policy.dto.PolicyComparisonResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 정책 비교는 별도로 저장하지 않는다. comparisonId는 비교 대상 policyId들을 오름차순 정렬해 "-"로 이어붙인 값이고, 조회 시 이를 다시 policyId
 * 목록으로 분해해 그때그때 최신 정책 정보를 내려준다.
 */
@Service
@RequiredArgsConstructor
public class PolicyComparisonService {

    private static final String ID_DELIMITER = "-";

    /**
     * 비교 가능한 정책 개수. {@link PolicyComparisonCreateRequest}의 {@code @Size(min, max)}와 같은 값이어야 한다 —
     * create로 만들 수 없는 comparisonId는 find로도 조회되면 안 되기 때문이다.
     */
    private static final int MIN_POLICY_COUNT = 2;

    private static final int MAX_POLICY_COUNT = 3;

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;

    @Transactional(readOnly = true)
    public PolicyComparisonResponse create(PolicyComparisonCreateRequest request) {
        List<Long> policyIds = List.copyOf(new TreeSet<>(request.policyIds()));
        return buildResponse(policyIds);
    }

    @Transactional(readOnly = true)
    public PolicyComparisonResponse find(String comparisonId) {
        return buildResponse(decode(comparisonId));
    }

    private PolicyComparisonResponse buildResponse(List<Long> policyIds) {
        List<Policy> policies = policyRepository.findAllById(policyIds);
        if (policies.size() != policyIds.size()) {
            throw new CustomException(PolicyErrorCode.POLICY_NOT_FOUND);
        }

        Map<Long, Policy> policyById =
                policies.stream().collect(Collectors.toMap(Policy::getId, Function.identity()));
        Map<Long, List<RegionResponse>> regionsByPolicyId = findRegionsByPolicyId(policyIds);
        List<PolicyComparisonItemResponse> items =
                policyIds.stream()
                        .map(
                                policyId ->
                                        PolicyComparisonItemResponse.from(
                                                policyById.get(policyId),
                                                regionsByPolicyId.getOrDefault(
                                                        policyId, List.of())))
                        .toList();

        return new PolicyComparisonResponse(encode(policyIds), items);
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

    private String encode(List<Long> policyIds) {
        return policyIds.stream().map(String::valueOf).collect(Collectors.joining(ID_DELIMITER));
    }

    /**
     * comparisonId를 policyId 목록으로 분해한다. create와 같은 개수 제약(2~3개)을 적용해, 생성할 수 없는 comparisonId는 조회도
     * 거부한다 — 임의로 긴 ID로 큰 IN 쿼리를 유발하는 것도 함께 막는다.
     */
    private List<Long> decode(String comparisonId) {
        try {
            List<Long> policyIds =
                    Arrays.stream(comparisonId.split(ID_DELIMITER)).map(Long::parseLong).toList();
            if (policyIds.size() < MIN_POLICY_COUNT || policyIds.size() > MAX_POLICY_COUNT) {
                throw new CustomException(PolicyErrorCode.COMPARISON_NOT_FOUND);
            }
            return policyIds;
        } catch (NumberFormatException e) {
            throw new CustomException(PolicyErrorCode.COMPARISON_NOT_FOUND);
        }
    }
}
