package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolicyRepository
        extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {

    long countByVisibilityAndDeletedAtIsNull(PolicyVisibility visibility);
}
