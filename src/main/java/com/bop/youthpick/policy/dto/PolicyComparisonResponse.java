package com.bop.youthpick.policy.dto;

import java.util.List;

public record PolicyComparisonResponse(
        String comparisonId, List<PolicyComparisonItemResponse> policies) {}
