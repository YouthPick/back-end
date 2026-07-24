package com.bop.youthpick.policy.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.policy.dto.PolicyRecentViewResponse;
import com.bop.youthpick.policy.service.PolicyRecentViewService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PolicyRecentViewController.class)
@AutoConfigureMockMvc(addFilters = false)
class PolicyRecentViewControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private PolicyRecentViewService policyRecentViewService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 최근_본_정책_목록은_200과_페이지_데이터를_반환한다() throws Exception {
        PolicyRecentViewResponse response =
                new PolicyRecentViewResponse(
                        10L,
                        "청년 월세 지원",
                        "설명",
                        "주거",
                        "중분류",
                        "국토교통부",
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 12, 31),
                        "https://example.com",
                        LocalDateTime.of(2026, 7, 16, 12, 0));
        Page<PolicyRecentViewResponse> page = new PageImpl<>(List.of(response));
        when(policyRecentViewService.getRecentPolicies(eq(1L), any(Pageable.class)))
                .thenReturn(page);
        authenticateAs(1L);

        mockMvc.perform(get("/api/v1/policy-recent-views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].policyId").value(10))
                .andExpect(jsonPath("$.data[0].title").value("청년 월세 지원"))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void 미인증_요청이면_401과_A001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/policy-recent-views"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
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
