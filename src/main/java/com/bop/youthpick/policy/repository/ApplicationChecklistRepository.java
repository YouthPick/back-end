package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationChecklistRepository extends JpaRepository<ApplicationChecklist, Long> {

    Optional<ApplicationChecklist> findByIdAndDeletedAtIsNull(Long id);

    List<ApplicationChecklist> findByApplication_IdAndDeletedAtIsNull(Long applicationId);
}
