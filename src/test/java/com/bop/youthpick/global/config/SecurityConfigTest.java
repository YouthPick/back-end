package com.bop.youthpick.global.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    void 정책_상세는_공개지만_정책_채팅_GET은_인증없이_접근하면_401이다() throws Exception {
        int detailStatus =
                mockMvc.perform(get("/api/v1/policies/1")).andReturn().getResponse().getStatus();
        assertThat(detailStatus).isNotIn(401, 403);

        mockMvc.perform(get("/api/v1/policies/1/chat/messages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void 정책_채팅_HTTP_POST는_더이상_존재하지_않는다() throws Exception {
        mockMvc.perform(
                        post("/api/v1/policies/1/chat/messages")
                                .contentType("application/json")
                                .content("{\"content\":\"메시지\"}"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void 게시글_조회는_인증없이_접근할_수_있다() throws Exception {
        int status = mockMvc.perform(get("/api/v1/posts")).andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void 게시글_작성은_인증없이_접근하면_401() throws Exception {
        mockMvc.perform(post("/api/v1/posts")).andExpect(status().isUnauthorized());
    }

    @Test
    void 파일_업로드는_인증없이_접근하면_401() throws Exception {
        mockMvc.perform(post("/api/v1/files")).andExpect(status().isUnauthorized());
    }

    @Test
    void 파일_조회는_인증없이_접근할_수_있다() throws Exception {
        int status =
                mockMvc.perform(get("/api/v1/files/00000000-0000-0000-0000-000000000000"))
                        .andReturn()
                        .getResponse()
                        .getStatus();

        assertThat(status).isNotIn(401, 403);
    }

    @Test
    void 회원_탈퇴_경로에_인증없이_접근하면_401() throws Exception {
        mockMvc.perform(delete("/api/v1/users")).andExpect(status().isUnauthorized());
    }

    @Test
    void 회원이_탈퇴_경로에_접근하면_인가는_통과한다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        int status =
                mockMvc.perform(
                                delete("/api/v1/users")
                                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                        .andReturn()
                        .getResponse()
                        .getStatus();

        // 컨트롤러가 아직 없어 401이 아닌 다른 상태로 응답한다 — 인가는 통과했다는 뜻이다.
        assertThat(status).isNotEqualTo(401);
    }
}
