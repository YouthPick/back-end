package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyApplicationChecklistRepository
        extends JpaRepository<PolicyApplicationChecklist, Long> {

    Optional<PolicyApplicationChecklist> findByIdAndDeletedAtIsNull(Long id);

    Page<PolicyApplicationChecklist> findByApplication_IdAndDeletedAtIsNull(
            Long applicationId, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query(
            "UPDATE PolicyApplicationChecklist c SET c.deletedAt = CURRENT_TIMESTAMP "
                    + "WHERE c.application.id = :applicationId AND c.deletedAt IS NULL")
    void softDeleteAllByApplicationId(@Param("applicationId") Long applicationId);
}
