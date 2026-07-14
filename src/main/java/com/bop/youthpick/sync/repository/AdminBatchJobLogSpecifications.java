package com.bop.youthpick.sync.repository;

import com.bop.youthpick.sync.entity.BatchStatus;
import com.bop.youthpick.sync.entity.PolicyBatchHistory;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 배치 작업 로그 목록 조회의 선택적 필터(status/기간)를 조합한다. */
public final class AdminBatchJobLogSpecifications {

    private AdminBatchJobLogSpecifications() {}

    public static Specification<PolicyBatchHistory> filter(
            String status, LocalDate startDate, LocalDate endDate) {
        return Specification.allOf(statusEquals(status), requestedBetween(startDate, endDate));
    }

    private static Specification<PolicyBatchHistory> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        BatchStatus value = BatchStatus.valueOf(status);
        return (root, query, cb) -> cb.equal(root.get("status"), value);
    }

    /** requestedAt이 [startDate, endDate] 날짜 범위 안에 있는 로그만 남긴다(KST 벽시계 시각 기준 직접 비교). */
    private static Specification<PolicyBatchHistory> requestedBetween(
            LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (startDate != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("requestedAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(
                        cb.lessThan(root.get("requestedAt"), endDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
