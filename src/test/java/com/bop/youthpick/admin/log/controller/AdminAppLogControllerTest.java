package com.bop.youthpick.admin.log.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.admin.log.dto.ApplicationLogResponse;
import com.bop.youthpick.admin.log.service.AdminAppLogService;
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

@WebMvcTest(controllers = AdminAppLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminAppLogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminAppLogService adminAppLogService;

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<ApplicationLogResponse> page =
                new PageImpl<>(
                        List.of(
                                new ApplicationLogResponse(
                                        1L,
                                        10L,
                                        "ERROR",
                                        "timeout",
                                        "trace-1",
                                        "GET",
                                        "/api/v1/policies",
                                        "127.0.0.1",
                                        "java.net.SocketTimeoutException",
                                        "connect timed out",
                                        "stack...",
                                        LocalDateTime.now())));
        when(adminAppLogService.search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/application-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].logLevel").value("ERROR"))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void logLevel_필터가_허용값이_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/application-logs").param("logLevel", "TRACE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void logLevel이_빈_문자열이면_400이_아니라_필터_없이_조회한다() throws Exception {
        when(adminAppLogService.search(eq(""), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/admin/application-logs").param("logLevel", ""))
                .andExpect(status().isOk());
    }
}
