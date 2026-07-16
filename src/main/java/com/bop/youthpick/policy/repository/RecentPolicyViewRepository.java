package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.RecentPolicyView;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecentPolicyViewRepository extends JpaRepository<RecentPolicyView, Long> {

    Optional<RecentPolicyView> findByUserIdAndPolicyId(Long userId, Long policyId);

    long countByUserId(Long userId);

    /** 보관 상한 초과분 정리용 — 오래된 것부터 잘라 온다. */
    List<RecentPolicyView> findByUserIdOrderByViewedAtAsc(Long userId, Pageable pageable);

    /** 삭제/숨김 처리된 정책은 목록에서 제외한다. */
    @Query(
            value =
                    """
                    select rpv from RecentPolicyView rpv
                    join fetch rpv.policy p
                    where rpv.user.id = :userId
                      and p.deletedAt is null
                      and p.visibility = com.bop.youthpick.policy.entity.PolicyVisibility.VISIBLE
                    order by rpv.viewedAt desc
                    """,
            countQuery =
                    """
                    select count(rpv) from RecentPolicyView rpv
                    join rpv.policy p
                    where rpv.user.id = :userId
                      and p.deletedAt is null
                      and p.visibility = com.bop.youthpick.policy.entity.PolicyVisibility.VISIBLE
                    """)
    Page<RecentPolicyView> findVisibleByUserId(@Param("userId") Long userId, Pageable pageable);
}
