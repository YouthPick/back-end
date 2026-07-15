package com.bop.youthpick.policy.repository;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
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
                deadlineBetween(deadlineStart, deadlineEnd),
                fetchPolicyAndUser());
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
        return (root, query, cb) -> cb.like(cb.lower(policyJoin(root).get("title")), pattern);
    }

    /** 목록 조회(content 쿼리)에서만 policy/user를 LEFT fetch join해 응답 매핑 시 N+1을 없앤다(count 쿼리는 건너뛴다). */
    private static Specification<PolicyApplication> fetchPolicyAndUser() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                policyJoin(root);
                root.fetch("user", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }

    /**
     * policyNameContains의 필터 join과 fetchPolicyAndUser의 fetch join이 동일한 "policy" 경로를 공유하도록 재사용한다.
     */
    @SuppressWarnings("unchecked")
    private static Join<PolicyApplication, Policy> policyJoin(Root<PolicyApplication> root) {
        for (Fetch<PolicyApplication, ?> fetch : root.getFetches()) {
            if (fetch.getAttribute().getName().equals("policy")) {
                return (Join<PolicyApplication, Policy>) fetch;
            }
        }
        for (Join<PolicyApplication, ?> join : root.getJoins()) {
            if (join.getAttribute().getName().equals("policy")) {
                return (Join<PolicyApplication, Policy>) join;
            }
        }
        return root.join("policy", JoinType.LEFT);
    }

    private static Specification<PolicyApplication> statusEquals(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        ApplicationStatus value = parseStatus(status);
        return (root, query, cb) -> cb.equal(root.get("status"), value);
    }

    private static ApplicationStatus parseStatus(String status) {
        try {
            return ApplicationStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new CustomException(PolicyErrorCode.INVALID_APPLICATION_STATUS);
        }
    }

    /**
     * deadline(endAt)이 [deadlineStart, deadlineEnd] 날짜 범위 안에 있는 신청만 남긴다. 이 프로젝트는 서버 전역에서 타임존 변환 없이
     * JVM 기본 타임존(KST로 고정 운영) 기준 LocalDateTime을 그대로 저장·비교하므로, 여기서도 같은 가정(KST 자정 경계)으로 비교한다.
     */
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
