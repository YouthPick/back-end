package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.PolicyApplication;
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
                userIdEquals(userId),
                policyNameContains(policyName),
                statusEquals(status),
                deadlineBetween(deadlineStart, deadlineEnd));
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
