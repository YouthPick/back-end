package com.bop.youthpick.sync.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.sync.exception.SyncErrorCode;
import com.bop.youthpick.sync.service.PolicySyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

// 권한(ADMIN) 검증은 SecurityConfig의 /api/v1/admin/** 경로 규칙이 담당 —
// 팀 컨벤션(AdminBatchJobLogControllerTest 등)대로 필터를 끄고 컨트롤러 동작만 검증한다.
@WebMvcTest(controllers = AdminPolicySyncController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPolicySyncControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicySyncService policySyncService;

    @Test
    void 수동_실행_요청은_비동기로_시작시키고_즉시_202를_반환한다() throws Exception {
        mockMvc.perform(post("/api/v1/admin/batch/policy-sync")).andExpect(status().isAccepted());

        verify(policySyncService).startFullSyncAsync();
    }

    @Test
    void 이미_실행_중이면_409와_SY001을_반환한다() throws Exception {
        doThrow(new CustomException(SyncErrorCode.SYNC_ALREADY_RUNNING))
                .when(policySyncService)
                .startFullSyncAsync();

        mockMvc.perform(post("/api/v1/admin/batch/policy-sync"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SY001"));
    }
}
