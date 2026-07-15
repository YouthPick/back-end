package com.bop.youthpick.sync.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.sync.dto.PolicySyncJobSummaryResponse;
import com.bop.youthpick.sync.service.AdminPolicySyncJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminPolicySyncJobController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPolicySyncJobControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminPolicySyncJobService adminPolicySyncJobService;

    @Test
    void 요약_조회는_200과_카운트_데이터를_반환한다() throws Exception {
        when(adminPolicySyncJobService.getSummary())
                .thenReturn(PolicySyncJobSummaryResponse.of(3241L, 12L));

        mockMvc.perform(get("/api/v1/admin/policy-sync-jobs/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.activeCount").value(3241))
                .andExpect(jsonPath("$.data.missingCount").value(12))
                .andExpect(jsonPath("$.data.parseErrorCount").value(0))
                .andExpect(jsonPath("$.data.dbFailCount").value(0));
    }
}
