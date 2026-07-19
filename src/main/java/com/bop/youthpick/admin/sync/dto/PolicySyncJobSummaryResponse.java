package com.bop.youthpick.admin.sync.dto;

/** parseErrorCount/dbFailCount는 정책 수집 배치 파이프라인이 아직 없어 항상 0이다. 파이프라인이 추가되면 실제 집계로 교체한다. */
public record PolicySyncJobSummaryResponse(
        long activeCount, long missingCount, long parseErrorCount, long dbFailCount) {

    public static PolicySyncJobSummaryResponse of(long activeCount, long missingCount) {
        return new PolicySyncJobSummaryResponse(activeCount, missingCount, 0, 0);
    }
}
