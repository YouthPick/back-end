package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationChecklistRepository extends JpaRepository<ApplicationChecklist, Long> {

    List<ApplicationChecklist> findByApplicationIdOrderByIdAsc(Long applicationId);
}
