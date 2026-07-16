package com.bop.youthpick.admin.sync.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.admin.sync.dto.BatchJobLogResponse;
import com.bop.youthpick.admin.sync.service.AdminBatchJobLogService;
import com.bop.youthpick.sync.entity.BatchStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminBatchJobLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminBatchJobLogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminBatchJobLogService adminBatchJobLogService;

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<BatchJobLogResponse> page =
                new PageImpl<>(
                        List.of(
                                new BatchJobLogResponse(
                                        1L,
                                        BatchStatus.SUCCEEDED,
                                        LocalDateTime.now(),
                                        10,
                                        5,
                                        2,
                                        0)));
        when(adminBatchJobLogService.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/batch-job-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.data[0].createdPolicyCount").value(10))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void status_필터가_허용값이_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/batch-job-logs").param("status", "PARTIAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }
}
