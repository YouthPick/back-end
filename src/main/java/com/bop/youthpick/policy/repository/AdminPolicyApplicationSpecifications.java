package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 정책 신청 목록 조회의 선택적 필터(userId/policyName/status/deadline 범위)를 조합한다. */
public final class AdminPolicyApplicationSpecifications {

    private AdminPolicyApplicationSpecifications() {}

    public static Specification<PolicyApplication> filter(
            Long userId,
            String policyName,
            String status,
            LocalDate deadlineStart,
            LocalDate deadlineEnd) {
        return Specification.allOf(
                notDeleted(),
                fetchPolicyAndUser(),
                userIdEquals(userId),
                policyNameContains(policyName),
                statusEquals(status),
                deadlineBetween(deadlineStart, deadlineEnd));
    }

    /** 관리 해제(soft delete)된 신청은 목록에서 제외한다. */
    private static Specification<PolicyApplication> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    /**
     * 목록 응답이 row마다 policy/user를 lazy 조회하는 N+1을 막기 위해 fetch join을 건다. count 쿼리(resultType=Long)에는
     * fetch가 의미 없고 오류를 유발할 수 있어 content 쿼리에만 적용한다.
     */
    private static Specification<PolicyApplication> fetchPolicyAndUser() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("policy", JoinType.LEFT);
                root.fetch("user", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    private static Specification<PolicyApplication> userIdEquals(Long userId) {
        if (userId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("user").get("id"), userId);
    }

    private static Specification<PolicyApplication> policyNameContains(String policyName) {
        if (policyName == null || policyName.isBlank()) {
            return null;
        }
        String pattern = "%" + policyName.toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.join("policy").get("title")), pattern);
    }

    private static Specification<PolicyApplication> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        ApplicationStatus value = ApplicationStatus.valueOf(status);
        return (root, query, cb) -> cb.equal(root.get("status"), value);
    }

    /** deadline(endAt)이 [deadlineStart, deadlineEnd] 날짜 범위 안에 있는 신청만 남긴다(KST 변환 없이 직접 비교). */
    private static Specification<PolicyApplication> deadlineBetween(
            LocalDate deadlineStart, LocalDate deadlineEnd) {
        if (deadlineStart == null && deadlineEnd == null) {
            return null;
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("endAt")));
            if (deadlineStart != null) {
                predicates.add(
                        cb.greaterThanOrEqualTo(root.get("endAt"), deadlineStart.atStartOfDay()));
            }
            if (deadlineEnd != null) {
                predicates.add(
                        cb.lessThan(root.get("endAt"), deadlineEnd.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
