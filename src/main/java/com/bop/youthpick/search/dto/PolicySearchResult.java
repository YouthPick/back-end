package com.bop.youthpick.search.dto;

import java.util.List;

/**
 * 검색 결과. <b>정책 id 와 총건수만</b> 돌려준다.
 *
 * <p>카드 내용을 ES 에서 함께 내리지 않는 이유: 그렇게 하면 ES 가 사실상 원본 행세를 하게 되어, 색인이 조금만 늦어도 사용자에게 옛 제목이 보인다.
 * id 만 받고 내용은 MySQL 에서 읽으면 화면에 보이는 값은 항상 원본 기준이고, 폴백 경로와 응답 조립 코드도 그대로 공유된다.
 *
 * @param policyIds 관련도 순으로 정렬된 정책 id
 * @param total 페이징용 전체 건수
 */
public record PolicySearchResult(List<Long> policyIds, long total) {

    public static PolicySearchResult empty() {
        return new PolicySearchResult(List.of(), 0);
    }
}
