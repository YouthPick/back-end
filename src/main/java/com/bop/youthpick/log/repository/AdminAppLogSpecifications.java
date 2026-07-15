package com.bop.youthpick.log.repository;

import com.bop.youthpick.log.entity.ApplicationLog;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 애플리케이션 로그 목록 조회의 선택적 필터(logLevel/keyword/기간)를 조합한다. */
public final class AdminAppLogSpecifications {

    private AdminAppLogSpecifications() {}

    public static Specification<ApplicationLog> filter(
            String logLevel, String keyword, LocalDate startDate, LocalDate endDate) {
        return Specification.allOf(
                levelEquals(logLevel),
                keywordContains(keyword),
                createdBetween(startDate, endDate));
    }

    private static Specification<ApplicationLog> levelEquals(String logLevel) {
        if (logLevel == null || logLevel.isBlank()) {
            return null;
        }
        // level은 enum이 아니라 자유 문자열 컬럼이라 적재 값의 대소문자를 보장할 수 없어 대소문자 무시 비교한다.
        return (root, query, cb) -> cb.equal(cb.upper(root.get("level")), logLevel.toUpperCase());
    }

    private static Specification<ApplicationLog> keywordContains(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String pattern = "%" + keyword.toLowerCase() + "%";
        return (root, query, cb) ->
                cb.or(
                        cb.like(cb.lower(root.get("message")), pattern),
                        cb.like(cb.lower(root.get("traceId")), pattern),
                        cb.like(cb.lower(root.get("uri")), pattern));
    }

    /** createdAt이 [startDate, endDate] 날짜 범위 안에 있는 로그만 남긴다(KST 벽시계 시각 기준 직접 비교). */
    private static Specification<ApplicationLog> createdBetween(
            LocalDate startDate, LocalDate endDate) {
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
