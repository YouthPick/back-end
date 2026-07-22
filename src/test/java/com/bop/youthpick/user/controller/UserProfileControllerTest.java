package com.bop.youthpick.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.dto.AuthPrincipal;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.service.UserProfileService;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UserProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserProfileControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private UserProfileService userProfileService;

    private static final String VALID_BODY =
            """
            {
                "birthYear": 2000,
                "regionCode": "11110",
                "employmentStatus": "EMPLOYED",
                "educationLevel": "UNIV_GRADUATE",
                "merryStatus": "SINGLE",
                "major": ["ENGINEERING"],
                "specialCondition": ["BASIC_LIVELIHOOD"],
                "income": 3000,
                "categories": ["취업", "주거"],
                "keywords": ["청년", "공모전"]
            }
            """;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 유효한_요청이면_201과_생성된_프로필을_반환한다() throws Exception {
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
        when(profile.getEducationLevel()).thenReturn("UNIV_GRADUATE");
        when(profile.getMerryStatus()).thenReturn("SINGLE");
        when(profile.getMajor()).thenReturn("ENGINEERING");
        when(profile.getSpecialCondition()).thenReturn("BASIC_LIVELIHOOD");
        when(profile.getIncome()).thenReturn(3000);
        when(profile.getCategories()).thenReturn("취업,주거");
        when(profile.getKeywords()).thenReturn("청년,공모전");
        when(profile.getStatus()).thenReturn("COMPLETED");
        when(userProfileService.submit(eq(1L), any())).thenReturn(profile);

        authenticateAs(1L);

        mockMvc.perform(
                        post("/api/v1/users/{userId}/profile", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.regionCode").value("11110"))
                .andExpect(jsonPath("$.data.merryStatus").value("SINGLE"))
                .andExpect(jsonPath("$.data.major[0]").value("ENGINEERING"))
                .andExpect(jsonPath("$.data.specialCondition[0]").value("BASIC_LIVELIHOOD"))
                .andExpect(jsonPath("$.data.income").value(3000))
                .andExpect(jsonPath("$.data.categories[0]").value("취업"))
                .andExpect(jsonPath("$.data.categories[1]").value("주거"));
    }

    @Test
    void 출생연도가_없으면_400과_C001을_반환한다() throws Exception {
        String invalidBody =
                """
                {
                    "regionCode": "11110",
                    "employmentStatus": "EMPLOYED",
                    "educationLevel": "UNIV_GRADUATE"
                }
                """;

        authenticateAs(1L);

        mockMvc.perform(
                        post("/api/v1/users/{userId}/profile", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 취업상태가_없으면_400과_C001을_반환한다() throws Exception {
        String invalidBody =
                """
                {
                    "birthYear": 2000,
                    "regionCode": "11110",
                    "educationLevel": "UNIV_GRADUATE"
                }
                """;

        authenticateAs(1L);

        mockMvc.perform(
                        post("/api/v1/users/{userId}/profile", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 어휘에_없는_취업상태면_400과_C001을_반환한다() throws Exception {
        // 검증이 없던 시절에는 이런 값도 그대로 저장돼, 맞춤정책 취업 축이 조용히 0점이 됐다.
        authenticateAs(1L);

        mockMvc.perform(
                        post("/api/v1/users/{userId}/profile", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY.replace("\"EMPLOYED\"", "\"MILITARY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("employmentStatus"));
    }

    @Test
    void 어휘에_없는_학력이면_400과_C001을_반환한다() throws Exception {
        authenticateAs(1L);

        mockMvc.perform(
                        post("/api/v1/users/{userId}/profile", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY.replace("\"UNIV_GRADUATE\"", "\"UNIVERSITY\"")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("educationLevel"));
    }

    @Test
    void 경로의_userId가_본인이_아니면_403과_A008을_반환한다() throws Exception {
        authenticateAs(1L);

        mockMvc.perform(
                        post("/api/v1/users/{userId}/profile", 2L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A008"));
    }

    @Test
    void 미인증_제출_요청이면_401과_A001을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/v1/users/{userId}/profile", 1L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
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
        when(profile.getEducationLevel()).thenReturn("UNIV_GRADUATE");
        when(profile.getMerryStatus()).thenReturn("SINGLE");
        when(profile.getMajor()).thenReturn("ENGINEERING");
        when(profile.getSpecialCondition()).thenReturn("BASIC_LIVELIHOOD");
        when(profile.getIncome()).thenReturn(3000);
        when(profile.getCategories()).thenReturn("취업,주거");
        when(profile.getKeywords()).thenReturn("청년,공모전");
        when(profile.getStatus()).thenReturn("COMPLETED");
        when(userProfileService.getMyProfile(1L)).thenReturn(profile);
        authenticateAs(1L);

        mockMvc.perform(get("/api/v1/me/profile"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.regionCode").value("11110"))
                .andExpect(jsonPath("$.data.income").value(3000));
    }

    @Test
    void 온보딩하지_않은_사용자면_200과_null_데이터를_반환한다() throws Exception {
        when(userProfileService.getMyProfile(1L)).thenReturn(null);
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

    @Test
    void 유효한_수정_요청이면_200과_수정된_프로필을_반환한다() throws Exception {
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
        when(userProfileService.update(eq(1L), any())).thenReturn(profile);
        authenticateAs(1L);

        mockMvc.perform(
                        patch("/api/v1/me/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.regionCode").value("11110"));
    }

    @Test
    void 미인증_수정_요청이면_401과_A001을_반환한다() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/me/profile")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(VALID_BODY))
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
