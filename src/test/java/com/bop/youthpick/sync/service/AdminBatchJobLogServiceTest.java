package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.sync.entity.PolicyBatchHistory;
import com.bop.youthpick.sync.repository.PolicyBatchHistoryRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminBatchJobLogServiceTest {

    @Mock private PolicyBatchHistoryRepository policyBatchHistoryRepository;

    private AdminBatchJobLogService adminBatchJobLogService;

    @BeforeEach
    void setUp() {
        adminBatchJobLogService = new AdminBatchJobLogService(policyBatchHistoryRepository);
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_페이지를_반환한다() {
        PolicyBatchHistory history = mock(PolicyBatchHistory.class);
        when(history.getId()).thenReturn(1L);
        Page<PolicyBatchHistory> page = new PageImpl<>(List.of(history));
        when(policyBatchHistoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<?> result =
                adminBatchJobLogService.search("SUCCEEDED", null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }
}
