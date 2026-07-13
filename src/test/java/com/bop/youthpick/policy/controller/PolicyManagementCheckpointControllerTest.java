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

import com.bop.youthpick.policy.dto.ApplicationChecklistResponse;
import com.bop.youthpick.policy.entity.ApplicationChecklist;
import com.bop.youthpick.policy.entity.ApplicationStatus;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyApplication;
import com.bop.youthpick.policy.service.PolicyManagementCheckpointService;
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

@WebMvcTest(controllers = PolicyManagementCheckpointController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyManagementCheckpointControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicyManagementCheckpointService checkPointService;

    private ApplicationChecklist checklist() {
        PolicyApplication application =
                PolicyApplication.register(
                        mock(User.class),
                        mock(Policy.class),
                        ApplicationStatus.APPLIED,
                        null,
                        null);
        return ApplicationChecklist.create(application, "제출 서류 준비");
    }

    @Test
    void add_유효한_요청이면_201과_생성된_체크리스트를_반환한다() throws Exception {
        when(checkPointService.add(1L, "제출 서류 준비")).thenReturn(checklist());

        String body =
                """
                {
                    "managementId": 1,
                    "message": "제출 서류 준비"
                }
                """;

        mockMvc.perform(
                        post("/api/checkpoints")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.message").value("제출 서류 준비"))
                .andExpect(jsonPath("$.data.checked").value(false));
    }

    @Test
    void add_message가_비어있으면_400과_C001을_반환한다() throws Exception {
        String body =
                """
                {
                    "managementId": 1,
                    "message": ""
                }
                """;

        mockMvc.perform(
                        post("/api/checkpoints")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void getByManagement_신청관리별_체크리스트_목록을_반환한다() throws Exception {
        Page<ApplicationChecklistResponse> page =
                new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(checkPointService.getByManagement(eq(1L), any())).thenReturn(page);

        mockMvc.perform(get("/api/checkpoints/management/{managementId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.page").value(0))
                .andExpect(jsonPath("$.meta.totalCount").value(0))
                .andExpect(jsonPath("$.meta.totalPages").value(0));
    }

    @Test
    void check_성공하면_200과_체크_완료_메시지를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/checkpoints/{id}/check", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("체크 완료"));

        verify(checkPointService).check(5L);
    }

    @Test
    void uncheck_성공하면_200과_체크_해제_완료_메시지를_반환한다() throws Exception {
        mockMvc.perform(patch("/api/checkpoints/{id}/uncheck", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("체크 해제 완료"));

        verify(checkPointService).uncheck(5L);
    }

    @Test
    void delete_성공하면_200과_체크리스트_삭제_완료_메시지를_반환한다() throws Exception {
        mockMvc.perform(delete("/api/checkpoints/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("체크리스트 삭제 완료"));

        verify(checkPointService).delete(5L);
    }
}
