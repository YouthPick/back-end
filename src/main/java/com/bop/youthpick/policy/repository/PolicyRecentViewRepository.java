package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyRecentView;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRecentViewRepository extends JpaRepository<PolicyRecentView, Long> {

    Optional<PolicyRecentView> findByUserIdAndPolicyId(Long userId, Long policyId);

    /** 삭제/숨김 처리된 정책은 목록에서 제외한다. */
    @Query(
            value =
                    """
                    select rpv from PolicyRecentView rpv
                    join fetch rpv.policy p
                    where rpv.user.id = :userId
                      and p.deletedAt is null
                      and p.visibility = com.bop.youthpick.policy.entity.PolicyVisibility.VISIBLE
                    order by rpv.viewedAt desc
                    """,
            countQuery =
                    """
                    select count(rpv) from PolicyRecentView rpv
                    join rpv.policy p
                    where rpv.user.id = :userId
                      and p.deletedAt is null
                      and p.visibility = com.bop.youthpick.policy.entity.PolicyVisibility.VISIBLE
                    """)
    Page<PolicyRecentView> findVisibleByUserId(@Param("userId") Long userId, Pageable pageable);
}
