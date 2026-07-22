package com.bop.youthpick.post.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 공개 목록 API의 category 검증은 {@code @Validated} 메서드 검증이라 standalone MockMvc로는 동작하지 않는다. 실제 dispatch
 * 경로(@SpringBootTest)로 확인한다 — 검증이 없던 시절에는 잘못된 값이 {@code PostCategory.valueOf}까지 내려가 500(S001) +
 * ERROR 로그가 됐다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PostControllerCategoryValidationTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void 어휘에_없는_카테고리로_목록을_조회하면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("category", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 유효한_카테고리로_목록을_조회하면_200을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("category", "FREE")).andExpect(status().isOk());
    }
}
