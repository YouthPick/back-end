package com.bop.youthpick.admin.user.repository;

import com.bop.youthpick.user.entity.LoginHistory;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 로그인 이력 목록 조회의 선택적 필터(userId/기간)를 조합한다. */
public final class AdminLoginHistorySpecifications {

    private AdminLoginHistorySpecifications() {}

    public static Specification<LoginHistory> filter(
            Long userId, LocalDate startDate, LocalDate endDate) {
        return Specification.allOf(userIdEquals(userId), createdBetween(startDate, endDate));
    }

    private static Specification<LoginHistory> userIdEquals(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    /** createdAt이 [startDate, endDate] 날짜 범위 안에 있는 이력만 남긴다(KST 벽시계 시각 기준 직접 비교). */
    private static Specification<LoginHistory> createdBetween(
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
