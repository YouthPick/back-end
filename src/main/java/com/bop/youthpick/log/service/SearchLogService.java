package com.bop.youthpick.log.service;

import com.bop.youthpick.log.entity.SearchLog;
import com.bop.youthpick.log.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchLogService {

    private final SearchLogRepository searchLogRepository;

    @Async
    @Transactional
    public void record(String query, int resultCount) {
        try {
            searchLogRepository.save(SearchLog.create(query, resultCount));
        } catch (RuntimeException exception) {
            log.warn("검색 로그 적재에 실패했습니다.", exception);
        }
    }
}
