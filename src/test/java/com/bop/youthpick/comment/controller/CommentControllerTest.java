package com.bop.youthpick.comment.controller;

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
import com.bop.youthpick.comment.dto.CommentResponse;
import com.bop.youthpick.comment.service.CommentService;
import com.bop.youthpick.global.common.ApiResponse;
import com.bop.youthpick.global.error.GlobalExceptionHandler;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@ExtendWith(MockitoExtension.class)
class CommentControllerTest {

    @Mock private CommentService commentService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CommentController controller = new CommentController(commentService);
        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new GlobalExceptionHandler())
                        .setCustomArgumentResolvers(new TestCurrentUserArgumentResolver())
                        .build();
    }

    @Test
    void 댓글을_생성하면_201을_반환한다() throws Exception {
        when(commentService.create(eq(1L), eq(3L), any())).thenReturn(response(10L, null, "내용"));

        mockMvc.perform(
                        post("/api/v1/posts/{postId}/comments", 3L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"content": "내용"}
                                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.content").value("내용"));
    }

    @Test
    void 내용이_비어있으면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        post("/api/v1/posts/{postId}/comments", 3L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"content": ""}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 게시글의_댓글_목록을_조회한다() throws Exception {
        when(commentService.getAllByPostId(3L))
                .thenReturn(ApiResponse.ok(List.of(response(10L, null, "내용"))));

        mockMvc.perform(get("/api/v1/posts/{postId}/comments", 3L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void 댓글을_수정하면_200을_반환한다() throws Exception {
        when(commentService.update(eq(1L), eq(10L), any()))
                .thenReturn(response(10L, null, "수정된 내용"));

        mockMvc.perform(
                        patch("/api/v1/posts/{postId}/comments/{commentId}", 3L, 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"content": "수정된 내용"}
                                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("수정된 내용"));
    }

    @Test
    void 수정_내용이_비어있으면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(
                        patch("/api/v1/posts/{postId}/comments/{commentId}", 3L, 10L)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                        {"content": ""}
                                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 댓글을_삭제한다() throws Exception {
        doNothing().when(commentService).delete(1L, 10L);

        mockMvc.perform(delete("/api/v1/posts/{postId}/comments/{commentId}", 3L, 10L))
                .andExpect(status().isOk());

        verify(commentService).delete(1L, 10L);
    }

    private CommentResponse response(Long id, Long parentId, String content) {
        return new CommentResponse(
                id, parentId, 1L, "작성자", content, LocalDateTime.now(), LocalDateTime.now());
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
