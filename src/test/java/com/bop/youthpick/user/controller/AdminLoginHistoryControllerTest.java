package com.bop.youthpick.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.user.dto.LoginHistoryResponse;
import com.bop.youthpick.user.service.AdminLoginHistoryService;
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

@WebMvcTest(controllers = AdminLoginHistoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminLoginHistoryControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminLoginHistoryService adminLoginHistoryService;

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<LoginHistoryResponse> page =
                new PageImpl<>(
                        List.of(
                                new LoginHistoryResponse(
                                        1L, 10L, LocalDateTime.now(), LocalDateTime.now())));
        when(adminLoginHistoryService.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/login-histories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].userId").value(10))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }
}
