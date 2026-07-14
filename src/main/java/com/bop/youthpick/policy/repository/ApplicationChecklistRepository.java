package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.ApplicationChecklist;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationChecklistRepository extends JpaRepository<ApplicationChecklist, Long> {

    /** soft delete된 항목은 제외하고, 화면에 일관된 순서로 보이도록 id 오름차순으로 정렬한다. */
    List<ApplicationChecklist> findByApplicationIdAndDeletedAtIsNullOrderByIdAsc(
            Long applicationId);
}
