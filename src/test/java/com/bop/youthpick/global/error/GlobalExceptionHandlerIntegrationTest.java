package com.bop.youthpick.global.error;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 프레임워크 표준 예외가 catch-all(S001 500)로 새지 않고 정확한 4xx + 에러코드로 내려가는지 실제 dispatch 경로로 확인한다. 개별 핸들러가 없던
 * 시절에는 아래 케이스가 전부 500 + ERROR 로그(application_logs 적재)였다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    @Test
    void 존재하지_않는_경로는_404_C007로_응답한다() throws Exception {
        mockMvc.perform(get("/api/v1/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("C007"));
    }

    @Test
    void 존재하지_않는_정렬_속성은_400_C001로_응답한다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        mockMvc.perform(
                        get("/api/v1/policy-applications")
                                .param("sort", "notAProperty")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("sort"))
                .andExpect(jsonPath("$.errors[0].value").value("notAProperty"));
    }

    @Test
    void 지원하지_않는_ContentType은_415_C008로_응답한다() throws Exception {
        String token = jwtTokenProvider.createAccessToken(1L, "USER");

        mockMvc.perform(
                        post("/api/v1/posts")
                                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("plain text body"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("C008"));
    }
}
