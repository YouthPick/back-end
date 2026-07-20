package com.bop.youthpick.user.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.service.OnboardingService;
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

@WebMvcTest(controllers = MyProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class MyProfileControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private OnboardingService onboardingService;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 온보딩된_사용자면_200과_프로필을_반환한다() throws Exception {
        UserProfile profile = mock(UserProfile.class);
        User user = mock(User.class);
        Region region = mock(Region.class);
        when(user.getId()).thenReturn(1L);
        when(region.getCode()).thenReturn("11110");
        when(profile.getId()).thenReturn(10L);
        when(profile.getUser()).thenReturn(user);
        when(profile.getRegion()).thenReturn(region);
        when(profile.getBirthYear()).thenReturn(2000);
        when(profile.getEmploymentStatus()).thenReturn("EMPLOYED");
        when(profile.getEducationLevel()).thenReturn("UNIVERSITY");
        when(profile.getMerryStatus()).thenReturn("SINGLE");
        when(profile.getMajor()).thenReturn("COMPUTER_SCIENCE");
        when(profile.getSpecialCondition()).thenReturn("LOW_INCOME");
        when(profile.getIncome()).thenReturn(3000);
        when(profile.getCategories()).thenReturn("취업,주거");
        when(profile.getKeywords()).thenReturn("청년,공모전");
        when(profile.getStatus()).thenReturn("COMPLETED");
        when(onboardingService.getMyProfile(1L)).thenReturn(profile);
        authenticateAs(1L);

        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.regionCode").value("11110"))
                .andExpect(jsonPath("$.data.income").value(3000));
    }

    @Test
    void 온보딩하지_않은_사용자면_200과_null_데이터를_반환한다() throws Exception {
        when(onboardingService.getMyProfile(1L)).thenReturn(null);
        authenticateAs(1L);

        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void 미인증_요청이면_401과_A001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/me/profile"))
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
