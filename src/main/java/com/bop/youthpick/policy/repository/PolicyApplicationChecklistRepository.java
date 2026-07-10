package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyApplicationChecklistRepository
        extends JpaRepository<ApplicationChecklist, Long> {

    Optional<ApplicationChecklist> findByIdAndDeletedAtIsNull(Long id);

    Page<ApplicationChecklist> findByApplication_IdAndDeletedAtIsNull(
            Long applicationId, Pageable pageable);
}
