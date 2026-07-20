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
import com.bop.youthpick.policy.dto.PolicyApplicationChecklistResponse;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import com.bop.youthpick.policy.service.PolicyApplicationChecklistService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyApplicationChecklistController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyApplicationChecklistControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicyApplicationChecklistService checklistService;

    private PolicyApplicationChecklist checklist() {
        PolicyApplication application =
                PolicyApplication.create(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.APPLIED,
                        null,
                        null);
        return PolicyApplicationChecklist.create(application, "제출 서류 준비");
    }

    @Test
    void add_유효한_요청이면_201과_생성된_체크리스트를_반환한다() throws Exception {
        when(checklistService.add(1L, 1L, "제출 서류 준비")).thenReturn(checklist());

        String body =
                """
                {
                    "applicationId": 1,
                    "message": "제출 서류 준비"
                }
                """;

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        post("/api/v1/policy-application-checklists")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.data.message").value("제출 서류 준비"))
                                .andExpect(jsonPath("$.data.checked").value(false)));
    }

    @Test
    void add_message가_비어있으면_400과_C001을_반환한다() throws Exception {
        String body =
                """
                {
                    "applicationId": 1,
                    "message": ""
                }
                """;

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        post("/api/v1/policy-application-checklists")
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
    }

    @Test
    void getByApplication_신청관리별_체크리스트_목록을_반환한다() throws Exception {
        Page<PolicyApplicationChecklistResponse> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(checklistService.getByApplication(eq(1L), eq(1L), any())).thenReturn(page);

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        get(
                                                "/api/v1/policy-application-checklists/application/{applicationId}",
                                                1L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data").isArray())
                                .andExpect(jsonPath("$.meta.page").value(1))
                                .andExpect(jsonPath("$.meta.totalCount").value(0))
                                .andExpect(jsonPath("$.meta.totalPages").value(0)));
    }

    @Test
    void update_유효한_요청이면_200과_수정된_체크리스트를_반환한다() throws Exception {
        PolicyApplicationChecklist updated = checklist();
        updated.updateContent("서류 다시 준비");
        when(checklistService.update(5L, 1L, "서류 다시 준비")).thenReturn(updated);

        String body =
                """
                {
                    "message": "서류 다시 준비"
                }
                """;

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/v1/policy-application-checklists/{id}", 5L)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value("서류 다시 준비")));
    }

    @Test
    void update_message가_비어있으면_400과_C001을_반환한다() throws Exception {
        String body =
                """
                {
                    "message": ""
                }
                """;

        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch("/api/v1/policy-application-checklists/{id}", 5L)
                                                .contentType(MediaType.APPLICATION_JSON)
                                                .content(body))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
    }

    @Test
    void check_성공하면_200과_체크_완료_메시지를_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch(
                                                "/api/v1/policy-application-checklists/{id}/check",
                                                5L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value("체크 완료")));

        verify(checklistService).check(5L, 1L);
    }

    @Test
    void uncheck_성공하면_200과_체크_해제_완료_메시지를_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch(
                                                "/api/v1/policy-application-checklists/{id}/uncheck",
                                                5L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value("체크 해제 완료")));

        verify(checklistService).uncheck(5L, 1L);
    }

    @Test
    void delete_성공하면_200과_체크리스트_삭제_완료_메시지를_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(delete("/api/v1/policy-application-checklists/{id}", 5L))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.data.message").value("체크리스트 삭제 완료")));

        verify(checklistService).delete(5L, 1L);
    }

    @Test
    void getByApplication_applicationId가_숫자가_아니면_400과_C001을_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        get(
                                                "/api/v1/policy-application-checklists/application/{applicationId}",
                                                "abc"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
    }

    @Test
    void check_id가_숫자가_아니면_400과_C001을_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(
                                        patch(
                                                "/api/v1/policy-application-checklists/{id}/check",
                                                "abc"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
    }

    @Test
    void delete_id가_숫자가_아니면_400과_C001을_반환한다() throws Exception {
        withAuthenticatedPrincipal(
                () ->
                        mockMvc.perform(delete("/api/v1/policy-application-checklists/{id}", "abc"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.code").value("C001")));
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
