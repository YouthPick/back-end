package com.bop.youthpick.user.repository;

import com.bop.youthpick.user.entity.Role;
import com.bop.youthpick.user.entity.User;
import org.springframework.data.jpa.domain.Specification;

/** 관리자 사용자 목록 조회의 선택적 필터(role/accountStatus/provider)를 조합한다. */
public final class AdminUserSpecifications {

    private AdminUserSpecifications() {}

    public static Specification<User> filter(String role, String accountStatus, String provider) {
        return Specification.allOf(
                roleEquals(role), accountStatusEquals(accountStatus), providerEquals(provider));
    }

    private static Specification<User> roleEquals(String role) {
        if (role == null || role.isBlank()) {
            return null;
        }
        Role roleValue = Role.valueOf(role);
        return (root, query, cb) -> cb.equal(root.get("role"), roleValue);
    }

    private static Specification<User> accountStatusEquals(String accountStatus) {
        if (accountStatus == null || accountStatus.isBlank()) {
            return null;
        }
        boolean deleted = "DELETED".equals(accountStatus);
        return (root, query, cb) ->
                deleted ? cb.isNotNull(root.get("deletedAt")) : cb.isNull(root.get("deletedAt"));
    }

    private static Specification<User> providerEquals(String provider) {
        if (provider == null || provider.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("provider"), provider);
    }
}
