package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;
import java.util.List;

/** 정책 상세 조회 응답 (유저 대상). 자격 판정용 내부 코드·보류 필드·raw payload는 노출하지 않는다. */
public record PolicyDetailResponse(
        Long id,
        String policyNo,
        String title,
        String description,
        String supportContent,
        String keywords,
        String category,
        String middleCategory,
        String organizationName,
        Integer minAge,
        Integer maxAge,
        String incomeConditionCode,
        Integer incomeMaxAmount,
        String incomeEtcContent,
        String additionalQualification,
        String participationRestriction,
        String applicationPeriodType,
        LocalDate applicationStartDate,
        LocalDate applicationEndDate,
        LocalDate businessPeriodBegin,
        LocalDate businessPeriodEnd,
        String businessPeriodEtc,
        Integer supportScaleCount,
        boolean firstComeFirstServed,
        String applicationUrl,
        String referenceUrl1,
        String referenceUrl2,
        String applicationMethod,
        String submissionDocuments,
        String screeningMethod,
        int viewCount,
        List<String> regionCodes) {

    public static PolicyDetailResponse from(Policy policy, List<String> regionCodes) {
        return new PolicyDetailResponse(
                policy.getId(),
                policy.getPolicyNo(),
                policy.getTitle(),
                policy.getDescription(),
                policy.getSupportContent(),
                policy.getKeywords(),
                policy.getCategory(),
                policy.getMiddleCategory(),
                policy.getOrganizationName(),
                policy.getMinAge(),
                policy.getMaxAge(),
                policy.getIncomeConditionCode(),
                policy.getIncomeMaxAmount(),
                policy.getIncomeEtcContent(),
                policy.getAdditionalQualification(),
                policy.getParticipationRestriction(),
                policy.getApplicationPeriodType(),
                policy.getApplicationStartDate(),
                policy.getApplicationEndDate(),
                policy.getBusinessPeriodBegin(),
                policy.getBusinessPeriodEnd(),
                policy.getBusinessPeriodEtc(),
                policy.getSupportScaleCount(),
                policy.isFirstComeFirstServed(),
                policy.getApplicationUrl(),
                policy.getReferenceUrl1(),
                policy.getReferenceUrl2(),
                policy.getApplicationMethod(),
                policy.getSubmissionDocuments(),
                policy.getScreeningMethod(),
                policy.getViewCount(),
                regionCodes);
    }
}
