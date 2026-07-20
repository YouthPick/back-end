package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /** 목록 카드 조회 — 노출 중이고 신청 마감(applicationEndDate)이 지나지 않은 정책만. 마감일 없음(상시)은 포함. */
    @Query(
            "select p from Policy p where p.visibility = :visibility and p.deletedAt is null"
                    + " and (p.applicationEndDate is null or p.applicationEndDate >= :today)")
    Page<Policy> findCards(
            @Param("visibility") PolicyVisibility visibility,
            @Param("today") LocalDate today,
            Pageable pageable);
}
