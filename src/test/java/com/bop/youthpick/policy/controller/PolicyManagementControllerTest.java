package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.service.PolicyManagementService;
import com.bop.youthpick.user.entity.User;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyManagementController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyManagementControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicyManagementService policyManagementService;

    @Test
    void 유효한_요청이면_201과_등록된_신청관리를_반환한다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        "메모",
                        null);
        when(policyManagementService.register(
                        eq(1L), eq(2L), eq(ApplicationStatus.INTERESTED), any(), any()))
                .thenReturn(application);

        String body =
                """
                {
                    "userId": 1,
                    "policyId": 2,
                    "status": "INTERESTED",
                    "memo": "메모"
                }
                """;

        mockMvc.perform(
                        post("/api/managements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("INTERESTED"));
    }

    @Test
    void status값이_유효하지_않으면_400과_C001을_반환한다() throws Exception {
        String body =
                """
                {
                    "userId": 1,
                    "policyId": 2,
                    "status": "UNKNOWN"
                }
                """;

        mockMvc.perform(
                        post("/api/managements")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void getManagements_사용자의_신청관리_목록을_반환한다() throws Exception {
        Page<PolicyApplicationResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(policyManagementService.getManagements(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/managements").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.totalCount").value(0));
    }

    @Test
    void changeStatus_유효한_요청이면_200과_변경된_상태를_반환한다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.APPLIED,
                        null,
                        null);
        when(policyManagementService.changeStatus(10L, ApplicationStatus.APPLIED))
                .thenReturn(application);

        mockMvc.perform(patch("/api/managements/{id}/status", 10L).param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"));
    }

    @Test
    void delete_성공하면_200을_반환한다() throws Exception {
        mockMvc.perform(delete("/api/managements/{id}", 10L)).andExpect(status().isOk());
    }
}
