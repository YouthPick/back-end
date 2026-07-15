package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolicyApplicationRepository
        extends JpaRepository<PolicyApplication, Long>,
                JpaSpecificationExecutor<PolicyApplication> {}
