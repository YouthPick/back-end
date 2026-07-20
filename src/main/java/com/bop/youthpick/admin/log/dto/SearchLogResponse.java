package com.bop.youthpick.admin.log.dto;

import com.bop.youthpick.log.entity.SearchLog;
import java.time.LocalDateTime;

public record SearchLogResponse(
        Long id,
        Long userId,
        String originalQuery,
        String normalizedQuery,
        int resultCount,
        LocalDateTime searchedAt) {
    public static SearchLogResponse from(SearchLog searchLog) {
        return new SearchLogResponse(
                searchLog.getId(),
                searchLog.getUserId(),
                searchLog.getQuery(),
                searchLog.getNormalized(),
                searchLog.getResultCount(),
                searchLog.getCreatedAt());
    }
}
