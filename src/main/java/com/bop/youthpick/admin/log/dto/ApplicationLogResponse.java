package com.bop.youthpick.admin.log.dto;

import com.bop.youthpick.log.entity.ApplicationLog;
import java.time.LocalDateTime;

public record ApplicationLogResponse(
        Long id,
        Long userId,
        String logLevel,
        String message,
        String traceId,
        String requestMethod,
        String requestUri,
        String userIp,
        String exceptionClass,
        String exceptionMessage,
        String stackTrace,
        LocalDateTime createdAt) {
    public static ApplicationLogResponse from(ApplicationLog appLog) {
        return new ApplicationLogResponse(
                appLog.getId(),
                appLog.getUserId(),
                appLog.getLevel(),
                appLog.getMessage(),
                appLog.getTraceId(),
                appLog.getMethod(),
                appLog.getUri(),
                appLog.getIp(),
                appLog.getExceptionClass(),
                appLog.getExceptionMessage(),
                appLog.getStackTrace(),
                appLog.getCreatedAt());
    }
}
