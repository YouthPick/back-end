package com.bop.youthpick.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OnboardingController.class)
@AutoConfigureMockMvc(addFilters = false)
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingService onboardingService;

    private static final String VALID_BODY = """
            {
                "birthYear": 2000,
                "regionCode": "11110",
                "employmentStatus": "EMPLOYED",
                "educationLevel": "UNIVERSITY",
                "categories": ["취업", "주거"],
                "keywords": ["청년", "공모전"]
            }
            """;

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
        when(profile.getEducationLevel()).thenReturn("UNIVERSITY");
        when(profile.getCategories()).thenReturn("취업,주거");
        when(profile.getKeywords()).thenReturn("청년,공모전");
        when(profile.getStatus()).thenReturn("COMPLETED");
        when(onboardingService.submit(eq(1L), any())).thenReturn(profile);

        mockMvc.perform(post("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.regionCode").value("11110"))
                .andExpect(jsonPath("$.data.categories[0]").value("취업"))
                .andExpect(jsonPath("$.data.categories[1]").value("주거"));
    }

    @Test
    void 출생연도가_없으면_400과_C001을_반환한다() throws Exception {
        String invalidBody = """
                {
                    "regionCode": "11110"
                }
                """;

        mockMvc.perform(post("/api/v1/users/{userId}/profile", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }
}
