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

/** 검색 로그 (0건율·인기검색어 집계 원본. 집계 후 기한 지나면 삭제). userId는 연관관계 없이 값만 — 비로그인 검색은 NULL. */
@Entity
@Table(
        name = "search_histories",
        indexes = @Index(name = "idx_search_histories_created", columnList = "created_at"))
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    /** 입력 원문 */
    @Column(length = 200, nullable = false)
    private String query;

    /** 소문자·공백정리 표준형 — 집계는 이 컬럼으로 */
    @Column(length = 200, nullable = false)
    private String normalized;

    /** 검색 결과 건수. 0 = 0건 검색(오타보정 발동 지표) */
    @Column(name = "result_count", nullable = false)
    private int resultCount;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static SearchLog create(String query, int resultCount) {
        SearchLog searchLog = new SearchLog();
        searchLog.query = query;
        searchLog.normalized = query.trim().toLowerCase();
        searchLog.resultCount = resultCount;
        return searchLog;
    }
}
