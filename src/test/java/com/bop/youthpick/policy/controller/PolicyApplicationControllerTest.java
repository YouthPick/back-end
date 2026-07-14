package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.policy.dto.PolicyApplicationResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.service.PolicyApplicationService;
import com.bop.youthpick.user.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyApplicationController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyApplicationControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicyApplicationService policyApplicationService;

    @Test
    void 유효한_요청이면_201과_등록된_신청관리를_반환한다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        "메모",
                        null);
        when(policyApplicationService.register(
                        eq(1L), eq(2L), eq(ApplicationStatus.INTERESTED), any(), any()))
                .thenReturn(application);

        String body =
                """
                {
                    "policyId": 2,
                    "status": "INTERESTED",
                    "memo": "메모"
                }
                """;

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        post("/api/applications")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.status").value("INTERESTED")));
    }

    @Test
    void 등록시_PREPARING도_유효한_상태값이다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.PREPARING,
                        null,
                        null);
        when(policyApplicationService.register(
                        eq(1L), eq(2L), eq(ApplicationStatus.PREPARING), any(), any()))
                .thenReturn(application);

        String body =
                """
                {
                    "policyId": 2,
                    "status": "PREPARING"
                }
                """;

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        post("/api/applications")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.status").value("PREPARING")));
    }

    @Test
    void status값이_유효하지_않으면_400과_C001을_반환한다() throws Exception {
        String body =
                """
                {
                    "policyId": 2,
                    "status": "UNKNOWN"
                }
                """;

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        post("/api/applications")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
    }

    @Test
    void getApplications_사용자의_신청관리_목록을_반환한다() throws Exception {
        Page<PolicyApplicationResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(policyApplicationService.getApplications(eq(1L), any())).thenReturn(page);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(get("/api/applications"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.meta.page").value(1))
                                .andExpect(jsonPath("$.meta.totalCount").value(0))
                                .andExpect(jsonPath("$.meta.totalPages").value(0)));
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
        when(policyApplicationService.changeStatus(10L, 1L, ApplicationStatus.APPLIED))
                .thenReturn(application);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/applications/{id}/status", 10L)
                                                .param("status", "APPLIED"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("APPLIED")));

        verify(policyApplicationService).changeStatus(10L, 1L, ApplicationStatus.APPLIED);
    }

    @Test
    void changeStatus_PREPARING도_유효한_상태값이다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.PREPARING,
                        null,
                        null);
        when(policyApplicationService.changeStatus(10L, 1L, ApplicationStatus.PREPARING))
                .thenReturn(application);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/applications/{id}/status", 10L)
                                                .param("status", "PREPARING"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.status").value("PREPARING")));
    }

    @Test
    void changeStatus_status값이_유효하지_않으면_400과_C001을_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/applications/{id}/status", 10L)
                                                .param("status", "UNKNOWN"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
    }

    @Test
    void updateMemo_유효한_요청이면_200과_수정된_메모를_반환한다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        "새 메모",
                        null);
        when(policyApplicationService.updateMemo(eq(10L), eq(1L), eq("새 메모")))
                .thenReturn(application);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/applications/{id}/memo", 10L)
                                                .param("memo", "새 메모"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.memo").value("새 메모")));
    }

    @Test
    void updateMemo_2000자를_초과하면_400과_C001을_반환한다() throws Exception {
        String tooLongMemo = "a".repeat(2001);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/applications/{id}/memo", 10L)
                                                .param("memo", tooLongMemo))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
    }

    @Test
    void updateEndAt_유효한_요청이면_200과_수정된_마감일을_반환한다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        null,
                        LocalDateTime.of(2026, 12, 31, 23, 59));
        when(policyApplicationService.updateEndAt(eq(10L), eq(1L), any())).thenReturn(application);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/applications/{id}/end-at", 10L)
                                                .param("endAt", "2026-12-31T23:59:00"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.endAt").value("2026-12-31T23:59:00")));
    }

    @Test
    void updateEndAt_파라미터를_생략하면_null로_비운다() throws Exception {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.INTERESTED,
                        null,
                        null);
        when(policyApplicationService.updateEndAt(eq(10L), eq(1L), eq(null)))
                .thenReturn(application);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(patch("/api/applications/{id}/end-at", 10L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.endAt").doesNotExist()));
    }

    @Test
    void delete_성공하면_200을_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(delete("/api/applications/{id}", 10L))
                                .andExpect(status().isOk()));

        verify(policyApplicationService).delete(10L, 1L);
    }

    /**
     * addFilters=false로 시큐리티 필터 체인(JwtAuthenticationFilter 포함)을 건너뛰므로, SecurityContextHolder를 직접
     * 채워 @CurrentUser를 해석시킨다.
     */
    private void withAuthenticatedPrincipal(ThrowingRunnable runnable) throws Exception {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(1L, "USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            runnable.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
