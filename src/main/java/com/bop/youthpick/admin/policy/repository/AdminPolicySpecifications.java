package com.bop.youthpick.admin.policy.repository;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 정책 목록 조회의 선택적 필터(category/visibilityStatus/신청기간 겹침)를 조합한다. */
public final class AdminPolicySpecifications {

    private AdminPolicySpecifications() {}

    public static Specification<Policy> filter(
            String category, String visibilityStatus, LocalDate startDate, LocalDate endDate) {
        return Specification.allOf(
                categoryEquals(category),
                visibilityEquals(visibilityStatus),
                applicationPeriodOverlaps(startDate, endDate));
    }

    private static Specification<Policy> categoryEquals(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    private static Specification<Policy> visibilityEquals(String visibilityStatus) {
        if (visibilityStatus == null || visibilityStatus.isBlank()) {
            return null;
        }
        if ("VISIBLE".equals(visibilityStatus)) {
            return (root, query, cb) ->
                    cb.and(
                            cb.equal(root.get("visibility"), PolicyVisibility.VISIBLE),
                            cb.isFalse(root.get("adminHidden")));
        }
        return (root, query, cb) ->
                cb.or(
                        cb.equal(root.get("visibility"), PolicyVisibility.HIDDEN),
                        cb.isTrue(root.get("adminHidden")));
    }

    /** 정책의 [applicationStartDate, applicationEndDate] 구간이 [startDate, endDate]와 겹치는 정책만 남긴다. */
    private static Specification<Policy> applicationPeriodOverlaps(
            LocalDate startDate, LocalDate endDate) {
        if (startDate == null && endDate == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("applicationStartDate")));
            predicates.add(cb.isNotNull(root.get("applicationEndDate")));
            if (endDate != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("applicationStartDate"), endDate));
            }
            if (startDate != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("applicationEndDate"), startDate));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
