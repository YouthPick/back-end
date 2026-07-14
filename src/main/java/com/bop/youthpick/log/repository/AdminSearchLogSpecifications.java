package com.bop.youthpick.log.repository;

import com.bop.youthpick.log.entity.SearchLog;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 검색 로그 목록 조회의 선택적 필터(keyword/기간)를 조합한다. */
public final class AdminSearchLogSpecifications {

    private AdminSearchLogSpecifications() {}

    public static Specification<SearchLog> filter(
            String keyword, LocalDate startDate, LocalDate endDate) {
        return Specification.allOf(keywordContains(keyword), createdBetween(startDate, endDate));
    }

    private static Specification<SearchLog> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("query")), pattern),
                        cb.like(cb.lower(root.get("normalized")), pattern));
    }

    /**
     * createdAt이 [startDate, endDate] 날짜 범위 안에 있는 로그만 남긴다. 이 코드베이스는 타임스탬프를 타임존 없는 {@code
     * LocalDateTime}으로만 저장하므로(UTC 저장 후 변환하는 구조가 아님) "KST 기준 기간 필터"는 저장값을 이미 KST 벽시계 시각으로 간주해 직접
     * 비교한다.
     */
    private static Specification<SearchLog> createdBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (startDate != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("createdAt"), startDate.atStartOfDay()));
            }
            if (endDate != null) {
                predicates.add(
                        cb.lessThan(root.get("createdAt"), endDate.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
