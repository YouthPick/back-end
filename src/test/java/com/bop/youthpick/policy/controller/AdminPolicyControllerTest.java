package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.AdminPolicyResponse;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.AdminPolicyService;
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

@WebMvcTest(controllers = AdminPolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminPolicyControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminPolicyService adminPolicyService;

    private static final AdminPolicyResponse POLICY_RESPONSE =
            new AdminPolicyResponse(
                    1L,
                    "P001",
                    "정책명",
                    "주관기관",
                    "설명",
                    "일자리",
                    "중분류",
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    "https://example.com",
                    0,
                    PolicyVisibility.VISIBLE,
                    List.of("11110"),
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null);

    private static final String VALID_UPDATE_BODY =
            """
            {
                "policyName": "정책명",
                "organizationName": "주관기관",
                "description": "설명",
                "largeCategory": "일자리",
                "middleCategory": "중분류",
                "applicationStartDate": "2026-01-01",
                "applicationEndDate": "2026-12-31",
                "applicationUrl": "https://example.com",
                "regionCodes": ["11110"]
            }
            """;

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<AdminPolicyResponse> page = new PageImpl<>(List.of(POLICY_RESPONSE));
        when(adminPolicyService.search(isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/policies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].regionCodes[0]").value("11110"))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void visibilityStatus_필터가_허용값이_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/policies").param("visibilityStatus", "GONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 정책_수정이_유효하면_200과_수정된_정책을_반환한다() throws Exception {
        when(adminPolicyService.update(eq(1L), any())).thenReturn(POLICY_RESPONSE);

        mockMvc.perform(
                        put("/api/v1/admin/policies/{policyId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.policyName").value("정책명"));
    }

    @Test
    void 정책명이_없으면_400과_C001을_반환한다() throws Exception {
        String invalidBody =
                """
                {
                    "organizationName": "주관기관",
                    "largeCategory": "일자리",
                    "applicationStartDate": "2026-01-01",
                    "applicationEndDate": "2026-12-31",
                    "regionCodes": []
                }
                """;

        mockMvc.perform(
                        put("/api/v1/admin/policies/{policyId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 수정_대상_정책이_없으면_404와_P001을_반환한다() throws Exception {
        when(adminPolicyService.update(eq(1L), any()))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        mockMvc.perform(
                        put("/api/v1/admin/policies/{policyId}", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_UPDATE_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    void 노출상태_변경이_유효하면_200을_반환한다() throws Exception {
        when(adminPolicyService.updateVisibility(eq(1L), eq("HIDDEN"))).thenReturn(POLICY_RESPONSE);

        mockMvc.perform(
                        patch("/api/v1/admin/policies/{policyId}/visibility", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"visibilityStatus\":\"HIDDEN\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void 노출상태_값이_허용범위가_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/admin/policies/{policyId}/visibility", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"visibilityStatus\":\"GONE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 삭제하면_200과_deletedAt이_채워진_정책을_반환한다() throws Exception {
        AdminPolicyResponse deleted =
                new AdminPolicyResponse(
                        1L,
                        "P001",
                        "정책명",
                        "주관기관",
                        "설명",
                        "일자리",
                        "중분류",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        "https://example.com",
                        0,
                        PolicyVisibility.VISIBLE,
                        List.of("11110"),
                        LocalDateTime.now(),
                        LocalDateTime.now(),
                        LocalDateTime.now());
        when(adminPolicyService.softDelete(1L)).thenReturn(deleted);

        mockMvc.perform(delete("/api/v1/admin/policies/{policyId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedAt").exists());
    }

    @Test
    void 삭제_대상이_없으면_404와_P001을_반환한다() throws Exception {
        when(adminPolicyService.softDelete(1L))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/admin/policies/{policyId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"));
    }
}
