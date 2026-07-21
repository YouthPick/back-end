package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyComparisonItemResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.PolicyComparisonService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
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
                    List.of(new RegionResponse("11680", "서울특별시", "강남구")));

    @Test
    void 유효한_요청이면_200과_비교결과를_반환한다() throws Exception {
        when(policyComparisonService.compare(List.of(1L, 2L))).thenReturn(List.of(ITEM));

        mockMvc.perform(get("/api/v1/policy-comparisons").param("policyIds", "1", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].policyId").value(1))
                .andExpect(jsonPath("$.data[0].regions[0].provinceName").value("서울특별시"))
                // 자격 판정용 내부 코드는 상세 응답과 동일하게 노출하지 않는다.
                .andExpect(jsonPath("$.data[0].jobCodes").doesNotExist())
                .andExpect(jsonPath("$.data[0].maritalStatusCode").doesNotExist());
    }

    @Test
    void 정책ID가_1개면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/policy-comparisons").param("policyIds", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 정책ID가_4개면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/policy-comparisons").param("policyIds", "1", "2", "3", "4"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void policyIds_파라미터를_생략하면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/policy-comparisons"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 숫자가_아닌_policyId면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/policy-comparisons").param("policyIds", "1", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 중복된_정책이면_400과_P008을_반환한다() throws Exception {
        when(policyComparisonService.compare(anyList()))
                .thenThrow(new CustomException(PolicyErrorCode.INVALID_COMPARISON_REQUEST));

        mockMvc.perform(get("/api/v1/policy-comparisons").param("policyIds", "1", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("P008"));
    }

    @Test
    void 존재하지_않는_정책이_섞여있으면_404와_P001을_반환한다() throws Exception {
        when(policyComparisonService.compare(anyList()))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/policy-comparisons").param("policyIds", "1", "2"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"));
    }
}
