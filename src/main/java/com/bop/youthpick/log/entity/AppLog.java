package com.bop.youthpick.log.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 앱 에러/요청 로그. userId는 연관관계 없이 값만 저장 —
 * 로그는 유저 삭제와 무관하게 보존하고, 비로그인/배치는 NULL.
 */
@Entity
@Table(name = "app_logs",
        indexes = @Index(name = "idx_app_logs_created", columnList = "created_at"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AppLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** INFO | WARN | ERROR */
    @Column(length = 10, nullable = false)
    private String level;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(length = 10)
    private String method;

    @Column(columnDefinition = "TEXT")
    private String uri;

    /** IPv6 최대 45자 */
    @Column(length = 45)
    private String ip;

    @Column(name = "exception_class", length = 255)
    private String exceptionClass;

    @Column(name = "exception_message", columnDefinition = "TEXT")
    private String exceptionMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @Column(name = "user_id")
    private Long userId;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
