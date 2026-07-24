package com.bop.youthpick.admin.sync.dto;

import com.bop.youthpick.sync.entity.BatchStatus;
import com.bop.youthpick.sync.entity.PolicyBatchHistory;
import java.time.LocalDateTime;

public record BatchJobLogResponse(
        Long id,
        BatchStatus status,
        LocalDateTime executedAt,
        int createdPolicyCount,
        int updatedPolicyCount,
        int disappearedPolicyCount,
        int failedPolicyCount) {
    public static BatchJobLogResponse from(PolicyBatchHistory history) {
        return new BatchJobLogResponse(
                history.getId(),
                history.getStatus(),
                history.getRequestedAt(),
                history.getNewCount(),
                history.getUpdatedCount(),
                history.getMissingCount(),
                history.getErrorCount());
    }
}
