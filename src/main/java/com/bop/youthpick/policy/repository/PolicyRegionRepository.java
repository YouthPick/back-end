package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyRegion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, Long> {

    List<PolicyRegion> findByPolicyIdIn(List<Long> policyIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PolicyRegion pr where pr.policy.id = :policyId")
    void deleteByPolicyId(@Param("policyId") Long policyId);
}
