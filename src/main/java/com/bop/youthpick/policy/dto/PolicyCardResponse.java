package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.Policy;
import java.time.LocalDate;
import java.util.List;

/**
 * 정책 목록(카드) 응답. 상세 필드는 {@link PolicyDetailResponse}로 분리 — 프론트 카드가 쓰는 필드만 내린다.
 *
 * <p>{@code provinces}는 정책이 적용되는 시도명 목록(중복 제거·정렬)이다. 지역 매핑이 없으면 빈 리스트 — "전국/OO 외 N" 같은 표시 문구 조립은
 * 프론트 책임이며, 상세({@link PolicyDetailResponse#regions()})와 같은 규칙을 쓴다.
 *
 * <p>자격 판정용 내부 코드(jobCodes/schoolCodes/majorCodes/specializationCodes/maritalStatusCode)는 목록 화면에서도
 * 프론트가 추천 하드필터(결혼·전공·신분 등 자격조건으로 부적격 정책 제외)를 적용할 수 있도록 상세와 동일하게 노출한다(#92).
 */
public record PolicyCardResponse(
        Long id,
        String title,
        String category,
        String description,
        Integer minAge,
        Integer maxAge,
        String jobCodes,
        String schoolCodes,
        String maritalStatusCode,
        String majorCodes,
        String specializationCodes,
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
                policy.getJobCodes(),
                policy.getSchoolCodes(),
                policy.getMaritalStatusCode(),
                policy.getMajorCodes(),
                policy.getSpecializationCodes(),
                policy.getApplicationEndDate(),
                provinces);
    }
}
