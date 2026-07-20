package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;

/**
 * 비교표 한 열(정책 한 건)의 응답. 자격요건 원문 노출용 필드를 그대로 나열해 프론트가 항목별로 나란히 비교할 수 있게 한다. 자동판정 전용 코드값(job/school
 * /major/specialization/maritalStatus)은 디코딩 매핑표가 없어 화면에는 안 쓰이지만, 원본 코드를 그대로 보존해 내려준다.
 */
public record PolicyComparisonItemResponse(
        Long policyId,
        String title,
        String category,
        String organizationName,
        Integer minAge,
        Integer maxAge,
        String jobCodes,
        String schoolCodes,
        String incomeConditionCode,
        Integer incomeMaxAmount,
        String incomeEtcContent,
        String maritalStatusCode,
        String majorCodes,
        String specializationCodes,
        String additionalQualification,
        String participationRestriction,
        LocalDate applicationEndDate,
        String applicationUrl) {

    public static PolicyComparisonItemResponse from(Policy policy) {
        return new PolicyComparisonItemResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getOrganizationName(),
                policy.getMinAge(),
                policy.getMaxAge(),
                policy.getJobCodes(),
                policy.getSchoolCodes(),
                policy.getIncomeConditionCode(),
                policy.getIncomeMaxAmount(),
                policy.getIncomeEtcContent(),
                policy.getMaritalStatusCode(),
                policy.getMajorCodes(),
                policy.getSpecializationCodes(),
                policy.getAdditionalQualification(),
                policy.getParticipationRestriction(),
                policy.getApplicationEndDate(),
                policy.getApplicationUrl());
    }
}
