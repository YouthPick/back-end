package com.bop.youthpick.search.dto;

import java.util.List;

/**
 * 필터 UI 에 함께 보여줄 항목별 건수.
 *
 * <p>각 목록은 <b>자기 자신의 필터를 뺀</b> 조건에서 센 값이다. 카테고리=주거를 고른 상태에서 카테고리 건수까지 주거로 걸러 세면 나머지 넷이 전부 0이 되어
 * 사용자가 다른 분류로 옮겨갈 수 없다(막다른 골목). 지역 건수는 반대로 주거 조건을 적용한 값이라야 "주거 정책 중 서울 139건"이 된다.
 *
 * <p>빈 목록은 "0건"이 아니라 <b>집계하지 못했다</b>는 뜻이다 — ES 가 죽으면 폴백 경로에는 집계가 없다.
 */
public record PolicyFacets(List<FacetCount> categories, List<FacetCount> regions) {

    public record FacetCount(String key, long count) {}

    public static PolicyFacets empty() {
        return new PolicyFacets(List.of(), List.of());
    }
}
