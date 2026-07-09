package com.bop.youthpick.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.service.JwtTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

/** API 명세서 권한 컬럼(관리자/회원/비회원)이 SecurityConfig에 올바르게 반영됐는지 확인한다. */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void 관리자_경로에_인증없이_접근하면_401_A001() throws Exception {
        mockMvc.perform(get("/api/v1/admin/policy-sync-jobs"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void 일반_회원이_관리자_경로에_접근하면_403_A008() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        mockMvc.perform(
                        get("/api/v1/admin/policy-sync-jobs")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("A008"));
    }

    @Test
    void 관리자가_관리자_경로에_접근하면_인가는_통과한다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, "ADMIN");

        int status =
                mockMvc.perform(
                                get("/api/v1/admin/policy-sync-jobs")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getStatus();

        // 컨트롤러가 아직 없어 401/403이 아닌 다른 상태로 응답한다 — 인가는 통과했다는 뜻이다.
        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void 회원_전용_경로에_인증없이_접근하면_401() throws Exception {
        mockMvc.perform(get("/api/v1/me/favorites")).andExpect(status().isUnauthorized());
    }

    @Test
    void 회원이_회원_전용_경로에_접근하면_인가는_통과한다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        int status =
                mockMvc.perform(
                                get("/api/v1/me/favorites")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void 공개_경로는_인증없이_접근해도_401_403이_아니다() throws Exception {
        int status = mockMvc.perform(get("/api/v1/policies")).andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }
}
