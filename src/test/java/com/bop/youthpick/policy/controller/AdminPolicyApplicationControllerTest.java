package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.AdminPolicyApplicationResponse;
import com.bop.youthpick.policy.dto.ApplicationChecklistItemResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.AdminPolicyApplicationService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminPolicyApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPolicyApplicationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminPolicyApplicationService adminPolicyApplicationService;

    private static final AdminPolicyApplicationResponse APPLICATION_RESPONSE =
            new AdminPolicyApplicationResponse(
                    1L,
                    10L,
                    20L,
                    "정책명",
                    ApplicationStatus.INTERESTED,
                    "메모",
                    LocalDate.of(2026, 12, 31),
                    LocalDateTime.now());

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<AdminPolicyApplicationResponse> page = new PageImpl<>(List.of(APPLICATION_RESPONSE));
        when(adminPolicyApplicationService.search(
                        isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/policy-applications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].policyName").value("정책명"))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void status_필터가_허용값이_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/policy-applications").param("status", "CLOSED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 체크리스트_조회는_200과_목록을_반환한다() throws Exception {
        when(adminPolicyApplicationService.getChecklist(1L))
                .thenReturn(
                        List.of(
                                new ApplicationChecklistItemResponse(
                                        100L, 1L, true, "주민등록등본 제출", LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/admin/policy-applications/{applicationId}/checklist", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(100))
                .andExpect(jsonPath("$.data[0].checked").value(true));
    }

    @Test
    void 체크리스트_조회_대상이_없으면_404와_P004를_반환한다() throws Exception {
        when(adminPolicyApplicationService.getChecklist(1L))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/policy-applications/{applicationId}/checklist", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P004"));
    }

    @Test
    void 상태_변경이_유효하면_200을_반환한다() throws Exception {
        when(adminPolicyApplicationService.updateStatus(eq(1L), eq("APPLIED")))
                .thenReturn(APPLICATION_RESPONSE);

        mockMvc.perform(
                        patch("/api/v1/admin/policy-applications/{applicationId}/status", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 상태_값이_허용범위가_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/policy-applications/{applicationId}/status", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 상태_변경_대상이_없으면_404와_P004를_반환한다() throws Exception {
        when(adminPolicyApplicationService.updateStatus(eq(1L), eq("APPLIED")))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_APPLICATION_NOT_FOUND));

        mockMvc.perform(
                        patch("/api/v1/admin/policy-applications/{applicationId}/status", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"APPLIED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P004"));
    }
}
