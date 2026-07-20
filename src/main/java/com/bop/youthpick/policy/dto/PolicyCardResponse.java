package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;
import java.util.List;

/**
 * 정책 목록(카드) 응답. 상세 필드는 {@link PolicyDetailResponse}로 분리 — 프론트 카드가 쓰는 필드만 내린다.
 *
 * <p>{@code provinces}는 정책이 적용되는 시도명 목록(중복 제거·정렬)이다. 지역 매핑이 없으면 빈 리스트 — "전국/OO 외 N" 같은 표시 문구 조립은
 * 프론트 책임이며, 상세({@link PolicyDetailResponse#regions()})와 같은 규칙을 쓴다.
 */
public record PolicyCardResponse(
        Long id,
        String title,
        String category,
        String description,
        Integer minAge,
        Integer maxAge,
        LocalDate applicationEndDate,
        List<String> provinces) {

    public static PolicyCardResponse from(Policy policy, List<String> provinces) {
        return new PolicyCardResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getDescription(),
                policy.getMinAge(),
                policy.getMaxAge(),
                policy.getApplicationEndDate(),
                provinces);
    }
}
