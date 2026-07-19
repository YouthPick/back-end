package com.bop.youthpick.admin.log.service;

import com.bop.youthpick.admin.log.dto.ApplicationLogResponse;
import com.bop.youthpick.admin.log.repository.AdminAppLogSpecifications;
import com.bop.youthpick.log.repository.AppLogRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminAppLogService {

    private final AppLogRepository appLogRepository;

    @Transactional(readOnly = true)
    public Page<ApplicationLogResponse> search(
            String logLevel,
            String keyword,
            LocalDate startDate,
            LocalDate endDate,
            Pageable pageable) {
        return appLogRepository
                .findAll(
                        AdminAppLogSpecifications.filter(logLevel, keyword, startDate, endDate),
                        pageable)
                .map(ApplicationLogResponse::from);
    }
}
