package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyComparisonCreateRequest;
import com.bop.youthpick.policy.dto.PolicyComparisonItemResponse;
import com.bop.youthpick.policy.dto.PolicyComparisonResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
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

    private final PolicyRepository policyRepository;

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
        List<PolicyComparisonItemResponse> items =
                policyIds.stream()
                        .map(policyById::get)
                        .map(PolicyComparisonItemResponse::from)
                        .toList();

        return new PolicyComparisonResponse(encode(policyIds), items);
    }

    private String encode(List<Long> policyIds) {
        return policyIds.stream().map(String::valueOf).collect(Collectors.joining(ID_DELIMITER));
    }

    private List<Long> decode(String comparisonId) {
        try {
            List<Long> policyIds =
                    Arrays.stream(comparisonId.split(ID_DELIMITER)).map(Long::parseLong).toList();
            if (policyIds.size() < 2) {
                throw new CustomException(PolicyErrorCode.COMPARISON_NOT_FOUND);
            }
            return policyIds;
        } catch (NumberFormatException e) {
            throw new CustomException(PolicyErrorCode.COMPARISON_NOT_FOUND);
        }
    }
}
