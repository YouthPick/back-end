package com.bop.youthpick.search.dto;

import java.util.List;

/**
 * 정책이 바뀌었음을 알리는 이벤트. 검색 색인을 갱신하는 데 쓴다.
 *
 * <p>발행하는 쪽(배치·관리자)은 트랜잭션 <b>안에서</b> 던지고, 받는 쪽({@code PolicySearchSyncListener})은 커밋된 뒤에
 * 처리한다. 커밋 전에 색인하면 이후 롤백됐을 때 MySQL에는 없는 내용이 ES에만 남는다.
 *
 * <p>이벤트로 한 겹 끊어둔 덕분에 발행하는 쪽은 Elasticsearch 를 전혀 모른다 — 배치는 저장만 알면 되고, ES 가 없는 환경에서도
 * 그대로 동작한다.
 *
 * @param policyIds 바뀐 정책들. 배치는 청크 단위로 묶어 던져 색인 요청이 건수만큼 늘지 않게 한다.
 * @param removed 삭제(색인에서 제거)면 true, 신규·수정이면 false
 */
public record PolicyChangedEvent(List<Long> policyIds, boolean removed) {

    public static PolicyChangedEvent updated(Long policyId) {
        return new PolicyChangedEvent(List.of(policyId), false);
    }

    public static PolicyChangedEvent updated(List<Long> policyIds) {
        return new PolicyChangedEvent(List.copyOf(policyIds), false);
    }

    public static PolicyChangedEvent removed(Long policyId) {
        return new PolicyChangedEvent(List.of(policyId), true);
    }
}
