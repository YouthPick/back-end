package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;
import java.util.List;

/**
 * 맞춤정책 조회 응답. {@link PolicyCardResponse}에 매칭 점수(score, REC 6축 합산·0~100)와 매칭된 축 목록(matchedAxes)을
 * 더한다. matchedAxes는 프론트가 추천 사유 문구를 조립하는 데 쓰는 원자료다.
 */
public record RecommendedPolicyResponse(
        Long id,
        String title,
        String category,
        String description,
        Integer minAge,
        Integer maxAge,
        LocalDate applicationEndDate,
        List<String> provinces,
        int score,
        List<String> matchedAxes) {

    public static RecommendedPolicyResponse of(
            Policy policy, List<String> provinces, int score, List<String> matchedAxes) {
        return new RecommendedPolicyResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getCategory(),
                policy.getDescription(),
                policy.getMinAge(),
                policy.getMaxAge(),
                policy.getApplicationEndDate(),
                provinces,
                score,
                matchedAxes);
    }
}
