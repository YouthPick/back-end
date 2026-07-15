package com.bop.youthpick.sync.repository;

import com.bop.youthpick.sync.entity.PolicyBatchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface PolicyBatchHistoryRepository
        extends JpaRepository<PolicyBatchHistory, Long>,
                JpaSpecificationExecutor<PolicyBatchHistory> {}
