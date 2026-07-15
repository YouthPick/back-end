package com.bop.youthpick.log.repository;

import com.bop.youthpick.log.entity.AppLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AppLogRepository
        extends JpaRepository<AppLog, Long>, JpaSpecificationExecutor<AppLog> {}
