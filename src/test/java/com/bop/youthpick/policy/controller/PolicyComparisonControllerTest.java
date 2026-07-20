package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyComparisonCreateRequest;
import com.bop.youthpick.policy.dto.PolicyComparisonItemResponse;
import com.bop.youthpick.policy.dto.PolicyComparisonResponse;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.PolicyComparisonService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyComparisonController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyComparisonControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicyComparisonService policyComparisonService;

    private static final PolicyComparisonItemResponse ITEM =
            new PolicyComparisonItemResponse(
                    1L,
                    "청년 월세 지원",
                    "주거",
                    "국토교통부",
                    19,
                    34,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);

    @Test
    void create_유효한_요청이면_201과_비교결과를_반환한다() throws Exception {
        when(policyComparisonService.create(new PolicyComparisonCreateRequest(List.of(1L, 2L))))
                .thenReturn(new PolicyComparisonResponse("1-2", List.of(ITEM)));

        String body = "{\"policyIds\":[1,2]}";

        mockMvc.perform(
                        post("/api/v1/policy-comparisons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.comparisonId").value("1-2"))
                .andExpect(jsonPath("$.data.policies[0].policyId").value(1));
    }

    @Test
    void create_정책ID가_1개면_400과_C001을_반환한다() throws Exception {
        String body = "{\"policyIds\":[1]}";

        mockMvc.perform(
                        post("/api/v1/policy-comparisons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void create_정책ID가_4개면_400과_C001을_반환한다() throws Exception {
        String body = "{\"policyIds\":[1,2,3,4]}";

        mockMvc.perform(
                        post("/api/v1/policy-comparisons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void create_정책ID가_비어있으면_400과_C001을_반환한다() throws Exception {
        String body = "{\"policyIds\":[]}";

        mockMvc.perform(
                        post("/api/v1/policy-comparisons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void create_존재하지_않는_정책이_섞여있으면_404와_P001을_반환한다() throws Exception {
        when(policyComparisonService.create(new PolicyComparisonCreateRequest(List.of(1L, 2L))))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        String body = "{\"policyIds\":[1,2]}";

        mockMvc.perform(
                        post("/api/v1/policy-comparisons")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    void find_존재하면_200과_비교결과를_반환한다() throws Exception {
        when(policyComparisonService.find(eq("1-2")))
                .thenReturn(new PolicyComparisonResponse("1-2", List.of(ITEM)));

        mockMvc.perform(get("/api/v1/policy-comparisons/{comparisonId}", "1-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.comparisonId").value("1-2"));
    }

    @Test
    void find_형식이_잘못되면_404와_P008을_반환한다() throws Exception {
        when(policyComparisonService.find(eq("bad-id")))
                .thenThrow(new CustomException(PolicyErrorCode.COMPARISON_NOT_FOUND));

        mockMvc.perform(get("/api/v1/policy-comparisons/{comparisonId}", "bad-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P008"));
    }
}
