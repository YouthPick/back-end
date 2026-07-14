package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, Long> {

    void deleteByPolicy(Policy policy);
}
