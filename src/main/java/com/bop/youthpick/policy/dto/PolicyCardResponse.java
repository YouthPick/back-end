package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;

/** 정책 목록(카드) 응답. 상세 필드는 {@link PolicyDetailResponse}로 분리 — 프론트 카드가 쓰는 필드만 내린다. */
public record PolicyCardResponse(
        Long id,
        String title,
        String category,
        String description,
        Integer minAge,
        Integer maxAge,
        LocalDate applicationEndDate,
        String regionLabel) {

    public static PolicyCardResponse from(Policy policy, String regionLabel) {
        return new PolicyCardResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getDescription(),
                policy.getMinAge(),
                policy.getMaxAge(),
                policy.getApplicationEndDate(),
                regionLabel);
    }
}
