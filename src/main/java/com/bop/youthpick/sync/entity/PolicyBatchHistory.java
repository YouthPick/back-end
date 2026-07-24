package com.bop.youthpick.sync.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 정책 수집 배치 작업 이력 + 실행 요청서(REQUESTED→RUNNING→...). 자체 시각(requested/started/finished)을 쓰므로 BaseEntity
 * 미상속. 이력은 삭제하지 않는다 — soft delete 없음.
 */
@Entity
@Table(
        name = "policy_batch_history",
        indexes = @Index(name = "idx_policy_batch_history_requested", columnList = "requested_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyBatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BatchMode mode;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private BatchStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "new_count", nullable = false)
    private int newCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "unchanged_count", nullable = false)
    private int unchangedCount;

    /** API에서 사라진 정책 수 → visibility=HIDDEN 처리 (정상 결과) */
    @Column(name = "missing_count", nullable = false)
    private int missingCount;

    /** 처리 실패로 건너뛴 정책 수 (실패 격리 — 상세는 경고 로그의 plcyNo) */
    @Column(name = "error_count", nullable = false)
    private int errorCount;

    @Column(name = "failure_message", length = 1000)
    private String failureMessage;

    private static final int FAILURE_MESSAGE_MAX = 1000;

    public static PolicyBatchHistory request(BatchMode mode) {
        PolicyBatchHistory history = new PolicyBatchHistory();
        history.mode = mode;
        history.status = BatchStatus.REQUESTED;
        history.requestedAt = LocalDateTime.now();
        return history;
    }

    public void start() {
        this.status = BatchStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void succeed(
            int newCount, int updatedCount, int unchangedCount, int missingCount, int errorCount) {
        this.status = BatchStatus.SUCCEEDED;
        this.finishedAt = LocalDateTime.now();
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.unchangedCount = unchangedCount;
        this.missingCount = missingCount;
        this.errorCount = errorCount;
    }

    /** 원인 불명 조기 실패용(예: API fetch 자체가 실패해 아무것도 처리 못 한 경우) — 카운트는 전부 0으로 남는다. */
    public void fail(String message) {
        this.status = BatchStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.failureMessage = truncate(message);
    }

    /** 실패율 초과 등 일부라도 처리된 뒤 실패 처리하는 경우 — succeed와 동일한 카운트를 함께 남긴다. */
    public void fail(
            String message,
            int newCount,
            int updatedCount,
            int unchangedCount,
            int missingCount,
            int errorCount) {
        this.status = BatchStatus.FAILED;
        this.finishedAt = LocalDateTime.now();
        this.failureMessage = truncate(message);
        this.newCount = newCount;
        this.updatedCount = updatedCount;
        this.unchangedCount = unchangedCount;
        this.missingCount = missingCount;
        this.errorCount = errorCount;
    }

    private static String truncate(String message) {
        return message != null && message.length() > FAILURE_MESSAGE_MAX
                ? message.substring(0, FAILURE_MESSAGE_MAX)
                : message;
    }
}
