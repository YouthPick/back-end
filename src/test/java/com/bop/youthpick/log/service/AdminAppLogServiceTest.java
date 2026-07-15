package com.bop.youthpick.log.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.log.entity.AppLog;
import com.bop.youthpick.log.repository.AppLogRepository;
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
class AdminAppLogServiceTest {

    @Mock private AppLogRepository appLogRepository;

    private AdminAppLogService adminAppLogService;

    @BeforeEach
    void setUp() {
        adminAppLogService = new AdminAppLogService(appLogRepository);
    }

    @Test
    void 목록_조회는_필터를_Specification으로_넘겨_페이지를_반환한다() {
        AppLog appLog = mock(AppLog.class);
        when(appLog.getId()).thenReturn(1L);
        Page<AppLog> page = new PageImpl<>(List.of(appLog));
        when(appLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<?> result =
                adminAppLogService.search("ERROR", "timeout", null, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }
}
