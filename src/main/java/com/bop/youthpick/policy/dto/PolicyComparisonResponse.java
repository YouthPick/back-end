package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.util.List;

public record PolicyComparisonResponse(
        String comparisonId, List<PolicyComparisonItemResponse> policies) {

    public static PolicyComparisonResponse of(String comparisonId, List<Policy> orderedPolicies) {
        return new PolicyComparisonResponse(
                comparisonId,
                orderedPolicies.stream().map(PolicyComparisonItemResponse::from).toList());
    }
}
