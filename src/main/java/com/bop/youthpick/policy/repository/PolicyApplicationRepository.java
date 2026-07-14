package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyApplication;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolicyApplicationRepository
        extends JpaRepository<PolicyApplication, Long>,
                JpaSpecificationExecutor<PolicyApplication> {

    /** soft delete(관리 해제)된 신청은 관리자 화면에서도 존재하지 않는 것으로 취급한다. */
    Optional<PolicyApplication> findByIdAndDeletedAtIsNull(Long id);
}
