package com.bop.youthpick.log.service;

import com.bop.youthpick.log.dto.SearchLogResponse;
import com.bop.youthpick.log.repository.AdminSearchLogSpecifications;
import com.bop.youthpick.log.repository.SearchLogRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminSearchLogService {

    private final SearchLogRepository searchLogRepository;

    @Transactional(readOnly = true)
    public Page<SearchLogResponse> search(
            String keyword, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return searchLogRepository
                .findAll(AdminSearchLogSpecifications.filter(keyword, startDate, endDate), pageable)
                .map(SearchLogResponse::from);
    }
}
