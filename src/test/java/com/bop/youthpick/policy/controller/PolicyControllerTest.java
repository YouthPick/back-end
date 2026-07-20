package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.service.PolicyService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicyService policyService;

    private static final PolicyDetailResponse DETAIL_RESPONSE =
            new PolicyDetailResponse(
                    1L,
                    "R2026001",
                    "청년 월세 지원",
                    "설명",
                    "지원내용",
                    "월세,주거",
                    "주거",
                    "중분류",
                    "국토교통부",
                    19,
                    34,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    LocalDate.of(2026, 1, 1),
                    LocalDate.of(2026, 12, 31),
                    null,
                    null,
                    null,
                    null,
                    false,
                    "https://example.com",
                    null,
                    null,
                    null,
                    null,
                    null,
                    0,
                    List.of(new RegionResponse("11680", "서울특별시", "강남구")));

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 비회원_상세_조회는_userId_없이_200과_상세를_반환한다() throws Exception {
        when(policyService.getDetail(eq(1L), isNull())).thenReturn(DETAIL_RESPONSE);

        mockMvc.perform(get("/api/v1/policies/{policyId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("청년 월세 지원"))
                .andExpect(jsonPath("$.data.regions[0].regionCode").value("11680"))
                .andExpect(jsonPath("$.data.regions[0].provinceName").value("서울특별시"))
                .andExpect(jsonPath("$.data.regions[0].districtName").value("강남구"));
    }

    @Test
    void 로그인_상태의_상세_조회는_principal의_userId가_전달된다() throws Exception {
        when(policyService.getDetail(1L, 7L)).thenReturn(DETAIL_RESPONSE);
        authenticateAs(7L);

        mockMvc.perform(get("/api/v1/policies/{policyId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    void 없는_정책이면_404와_P001을_반환한다() throws Exception {
        when(policyService.getDetail(eq(99L), isNull()))
                .thenThrow(new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        mockMvc.perform(get("/api/v1/policies/{policyId}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("P001"));
    }

    /** addFilters=false로 시큐리티 필터를 건너뛰므로 SecurityContextHolder를 직접 채운다. */
    private void authenticateAs(Long userId) {
        Authentication authentication =
                new UsernamePasswordAuthenticationToken(
                        new AuthPrincipal(userId, "USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
