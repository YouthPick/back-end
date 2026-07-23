package com.bop.youthpick.post.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.global.error.GlobalExceptionHandler;
import com.bop.youthpick.post.dto.PostDetailResponse;
import com.bop.youthpick.post.dto.PostSummaryResponse;
import com.bop.youthpick.post.service.PostService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class PostControllerTest {

    @Mock private PostService postService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        PostController controller = new PostController(postService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(
                                new TestCurrentUserArgumentResolver(),
                                new PageableHandlerMethodArgumentResolver())
                        .build();
    }

    @Test
    void 본문이_1만자를_넘으면_400과_C001을_반환한다() throws Exception {
        String longContent = "가".repeat(10_001);
        String body =
                """
                {"category": "FREE", "title": "제목", "content": "%s"}
                """
                        .formatted(longContent);

        mockMvc.perform(post("/api/v1/posts").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 게시글을_생성하면_201을_반환한다() throws Exception {
        when(postService.create(eq(1L), any())).thenReturn(detail(3L, "FREE", "제목", "내용"));

        mockMvc.perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "category": "FREE",
                                          "title": "제목",
                                          "content": "내용"
                                        }
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(3))
                .andExpect(jsonPath("$.data.title").value("제목"));
    }

    @Test
    void 제목이_비어있으면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "category": "FREE",
                                          "title": "",
                                          "content": "내용"
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 첨부_URL의_UUID_형식이_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/v1/posts")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "category": "FREE",
                                          "title": "제목",
                                          "content": "내용",
                                          "attachmentUrls": ["/api/v1/files/------------------------------------"]
                                        }
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 게시글_목록과_페이지_정보를_조회한다() throws Exception {
        PostSummaryResponse summary =
                new PostSummaryResponse(3L, 1L, "작성자", null, null, "FREE", "제목", "내용", 0, null);
        when(postService.findAll(eq(null), eq(null), any()))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void 카테고리와_검색어_파라미터를_그대로_서비스에_전달한다() throws Exception {
        PostSummaryResponse summary =
                new PostSummaryResponse(3L, 1L, "작성자", null, null, "FREE", "잡담글", "내용", 0, null);
        when(postService.findAll(eq("FREE"), eq("잡담"), any()))
                .thenReturn(new PageImpl<>(List.of(summary)));

        mockMvc.perform(get("/api/v1/posts").param("category", "FREE").param("query", "잡담"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("잡담글"));

        verify(postService).findAll(eq("FREE"), eq("잡담"), any());
    }

    @Test
    void 유효하지_않은_목록_카테고리는_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/posts").param("category", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 게시글_상세를_조회한다() throws Exception {
        when(postService.findById(eq(3L), eq(1L), any()))
                .thenReturn(detail(3L, "FREE", "제목", "내용"));

        mockMvc.perform(get("/api/v1/posts/{postId}", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    void 게시글을_수정한다() throws Exception {
        when(postService.update(eq(1L), eq(3L), any()))
                .thenReturn(detail(3L, "FREE", "수정 후", "수정 후 내용"));

        mockMvc.perform(
                        patch("/api/v1/posts/{postId}", 3L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {
                                          "category": "FREE",
                                          "title": "수정 후",
                                          "content": "수정 후 내용"
                                        }
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("수정 후"));
    }

    @Test
    void 게시글을_삭제한다() throws Exception {
        doNothing().when(postService).delete(1L, 3L);

        mockMvc.perform(delete("/api/v1/posts/{postId}", 3L)).andExpect(status().isOk());

        verify(postService).delete(1L, 3L);
    }

    private PostDetailResponse detail(Long id, String category, String title, String content) {
        return new PostDetailResponse(
                id, 1L, "작성자", null, null, category, title, content, 0, null, null);
    }

    private static class TestCurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

        @Override
        public boolean supportsParameter(MethodParameter parameter) {
            return parameter.hasParameterAnnotation(CurrentUser.class);
        }

        @Override
        public Object resolveArgument(
                MethodParameter parameter,
                ModelAndViewContainer mavContainer,
                NativeWebRequest webRequest,
                org.springframework.web.bind.support.WebDataBinderFactory binderFactory) {
            return 1L;
        }
    }
}
