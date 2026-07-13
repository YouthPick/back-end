package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyApplication;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyApplicationRepository extends JpaRepository<PolicyApplication, Long> {

    Optional<PolicyApplication> findByIdAndDeletedAtIsNull(Long id);

    @EntityGraph(attributePaths = "policy")
    Page<PolicyApplication> findByUser_IdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Optional<PolicyApplication> findByUser_IdAndPolicy_Id(Long userId, Long policyId);
}
