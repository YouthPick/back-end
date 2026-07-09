package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyApplication;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyApplicationRepository extends JpaRepository<PolicyApplication, Long> {

    Optional<PolicyApplication> findByIdAndDeletedAtIsNull(Long id);

    List<PolicyApplication> findByUser_IdAndDeletedAtIsNull(Long userId);

    Optional<PolicyApplication> findByUser_IdAndPolicy_Id(Long userId, Long policyId);
}
