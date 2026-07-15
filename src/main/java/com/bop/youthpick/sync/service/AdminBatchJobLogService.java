package com.bop.youthpick.sync.service;

import com.bop.youthpick.sync.dto.BatchJobLogResponse;
import com.bop.youthpick.sync.repository.AdminBatchJobLogSpecifications;
import com.bop.youthpick.sync.repository.PolicyBatchHistoryRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminBatchJobLogService {

    private final PolicyBatchHistoryRepository policyBatchHistoryRepository;

    @Transactional(readOnly = true)
    public Page<BatchJobLogResponse> search(
            String status, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return policyBatchHistoryRepository
                .findAll(
                        AdminBatchJobLogSpecifications.filter(status, startDate, endDate), pageable)
                .map(BatchJobLogResponse::from);
    }
}
