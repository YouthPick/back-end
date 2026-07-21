package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;
import java.util.List;

/**
 * 비교표 한 열(정책 한 건)의 응답. 자격요건 원문 노출용 필드를 나열해 프론트가 항목별로 나란히 비교할 수 있게 한다.
 *
 * <p>필드 구성은 {@link PolicyDetailResponse}의 부분집합으로 유지한다 — 같은 정책을 상세로 볼 때는 감춰지는 값이 비교로 볼 때만 드러나면 안 되기
 * 때문이다. 따라서 자격 판정용 내부 코드(jobCodes/schoolCodes/majorCodes/specializationCodes/maritalStatusCode)와 보류
 * 필드, raw payload는 상세와 동일하게 노출하지 않는다.
 */
public record PolicyComparisonItemResponse(
        Long policyId,
        String title,
        String category,
        String organizationName,
        Integer minAge,
        Integer maxAge,
        String incomeConditionCode,
        Integer incomeMaxAmount,
        String incomeEtcContent,
        String additionalQualification,
        String participationRestriction,
        LocalDate applicationEndDate,
        String applicationUrl,
        List<RegionResponse> regions) {

    public static PolicyComparisonItemResponse from(Policy policy, List<RegionResponse> regions) {
        return new PolicyComparisonItemResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getOrganizationName(),
                policy.getMinAge(),
                policy.getMaxAge(),
                policy.getIncomeConditionCode(),
                policy.getIncomeMaxAmount(),
                policy.getIncomeEtcContent(),
                policy.getAdditionalQualification(),
                policy.getParticipationRestriction(),
                policy.getApplicationEndDate(),
                policy.getApplicationUrl(),
                regions);
    }
}
