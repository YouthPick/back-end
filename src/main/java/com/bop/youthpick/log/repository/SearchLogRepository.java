package com.bop.youthpick.log.repository;

import com.bop.youthpick.log.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface SearchLogRepository
        extends JpaRepository<SearchLog, Long>, JpaSpecificationExecutor<SearchLog> {}
