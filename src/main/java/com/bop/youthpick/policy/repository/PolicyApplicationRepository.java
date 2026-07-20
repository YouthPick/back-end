package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyApplication;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyApplicationRepository
        extends JpaRepository<PolicyApplication, Long>,
                JpaSpecificationExecutor<PolicyApplication> {

    // PolicyApplicationService.findActive(id)가 이걸 감싼다 — changeStatus/updateMemo/updateEndAt/delete가
    // 모두 이 메서드를 거쳐 "존재하지 않거나 이미 삭제된 id"를 POLICY_APPLICATION_NOT_FOUND로 통일해서 처리한다.
    Optional<PolicyApplication> findByIdAndDeletedAtIsNull(Long id);

    // PolicyApplicationService.getApplications()(목록 조회)에서 사용. @EntityGraph로 policy 연관관계를 함께
    // fetch해서,
    // PolicyApplicationResponse.from()이 policy.getTitle() 등을 부를 때 N+1 쿼리가 나지 않게 한다.
    @EntityGraph(attributePaths = "policy")
    Page<PolicyApplication> findByUser_IdAndDeletedAtIsNull(Long userId, Pageable pageable);

    // @SQLRestriction("deleted_at IS NULL")은 모든 엔티티 조회에 붙을 수 있으므로, 재등록 판단은 native query로
    // 명시적으로 우회한다. soft-delete된 기존 행까지 포함해야 UNIQUE(user_id, policy_id) 충돌 없이 reactivate할 수 있다.
    @Query(
            value =
                    "SELECT * FROM policy_applications WHERE user_id = :userId AND policy_id = :policyId LIMIT 1",
            nativeQuery = true)
    Optional<PolicyApplication> findIncludingDeletedByUserIdAndPolicyId(
            @Param("userId") Long userId, @Param("policyId") Long policyId);
}
