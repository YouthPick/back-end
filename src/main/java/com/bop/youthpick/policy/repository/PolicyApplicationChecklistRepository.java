package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyApplicationChecklistRepository
        extends JpaRepository<ApplicationChecklist, Long> {

    Optional<ApplicationChecklist> findByIdAndDeletedAtIsNull(Long id);

    Page<ApplicationChecklist> findByApplication_IdAndDeletedAtIsNull(
            Long applicationId, Pageable pageable);

    @Modifying(flushAutomatically = true)
    @Query(
            "UPDATE ApplicationChecklist c SET c.deletedAt = CURRENT_TIMESTAMP "
                    + "WHERE c.application.id = :applicationId AND c.deletedAt IS NULL")
    void softDeleteAllByApplicationId(@Param("applicationId") Long applicationId);
}
