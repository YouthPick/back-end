package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, Long> {

    void deleteByPolicy(Policy policy);

    List<PolicyRegion> findByPolicyIdIn(List<Long> policyIds);

    /** 목록 카드의 지역 라벨 조립용 — 전국 정책은 지역이 최대 256행이라 lazy 초기화(N+1)를 fetch join으로 막는다. */
    @Query("select pr from PolicyRegion pr join fetch pr.region where pr.policy.id in :policyIds")
    List<PolicyRegion> findWithRegionByPolicyIdIn(@Param("policyIds") List<Long> policyIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from PolicyRegion pr where pr.policy.id = :policyId")
    void deleteByPolicyId(@Param("policyId") Long policyId);
}
