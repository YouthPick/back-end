package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface PolicyRepository
        extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {

    /** 배치 비교용 전체 스냅샷 — HIDDEN 포함 (제외 이유는 {@link PolicySyncSnapshot} 참고). */
    @Query(
            "select new com.bop.youthpick.policy.dto.PolicySyncSnapshot("
                    + "p.policyNo, p.lastModifiedAt, p.visibility) from Policy p")
    List<PolicySyncSnapshot> findSyncSnapshots();

    List<Policy> findByPolicyNoIn(Collection<String> policyNos);

    long countByVisibilityAndDeletedAtIsNull(PolicyVisibility visibility);

    Optional<Policy> findByIdAndVisibilityAndDeletedAtIsNull(Long id, PolicyVisibility visibility);
}
