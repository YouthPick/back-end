package com.bop.youthpick.admin.log.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.admin.log.dto.SearchLogResponse;
import com.bop.youthpick.admin.log.service.AdminSearchLogService;
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

@WebMvcTest(controllers = AdminSearchLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminSearchLogControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminSearchLogService adminSearchLogService;

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<SearchLogResponse> page =
                new PageImpl<>(
                        List.of(
                                new SearchLogResponse(
                                        1L, 10L, "청년 정책", "청년정책", 5, LocalDateTime.now())));
        when(adminSearchLogService.search(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/search-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].originalQuery").value("청년 정책"))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }
}
