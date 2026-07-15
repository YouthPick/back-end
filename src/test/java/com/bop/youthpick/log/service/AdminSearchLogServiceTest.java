package com.bop.youthpick.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.log.entity.SearchLog;
import com.bop.youthpick.log.repository.SearchLogRepository;
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
class AdminSearchLogServiceTest {

    @Mock private SearchLogRepository searchLogRepository;

    private AdminSearchLogService adminSearchLogService;

    @BeforeEach
    void setUp() {
        adminSearchLogService = new AdminSearchLogService(searchLogRepository);
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_페이지를_반환한다() {
        SearchLog searchLog = mock(SearchLog.class);
        when(searchLog.getId()).thenReturn(1L);
        Page<SearchLog> page = new PageImpl<>(List.of(searchLog));
        when(searchLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<?> result = adminSearchLogService.search("청년", null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }
}
