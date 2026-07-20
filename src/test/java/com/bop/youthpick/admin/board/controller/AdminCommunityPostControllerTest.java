package com.bop.youthpick.admin.board.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.admin.board.dto.AdminAttachmentResponse;
import com.bop.youthpick.admin.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.admin.board.dto.AdminCommunityPostResponse;
import com.bop.youthpick.admin.board.service.AdminCommunityService;
import com.bop.youthpick.board.exception.BoardErrorCode;
import com.bop.youthpick.board.exception.BoardException;
import com.bop.youthpick.post.entity.PostCategory;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminCommunityPostController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCommunityPostControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminCommunityService adminCommunityService;

    private static final AdminCommunityPostResponse POST_RESPONSE =
            new AdminCommunityPostResponse(
                    1L,
                    "제목",
                    PostCategory.QUESTION,
                    "내용",
                    10L,
                    "닉네임",
                    LocalDateTime.now(),
                    0,
                    null);

    @Test
    void 목록_조회는_200과_페이지_데이터를_반환한다() throws Exception {
        Page<AdminCommunityPostResponse> page = new PageImpl<>(List.of(POST_RESPONSE));
        when(adminCommunityService.searchPosts(
                        isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/community-posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].authorName").value("닉네임"))
                .andExpect(jsonPath("$.meta.totalCount").value(1));
    }

    @Test
    void category_필터가_허용값이_아니면_400과_C001을_반환한다() throws Exception {
        mockMvc.perform(get("/api/v1/admin/community-posts").param("category", "NOTICE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void 댓글_조회는_200과_목록을_반환한다() throws Exception {
        when(adminCommunityService.getComments(1L))
                .thenReturn(
                        List.of(
                                new AdminCommunityCommentResponse(
                                        2L, 1L, null, "닉네임", "댓글", LocalDateTime.now(), null)));

        mockMvc.perform(get("/api/v1/admin/community-posts/{postId}/comments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(2));
    }

    @Test
    void 댓글_조회_대상_게시글이_없으면_404와_B001을_반환한다() throws Exception {
        when(adminCommunityService.getComments(1L))
                .thenThrow(new BoardException(BoardErrorCode.POST_NOT_FOUND));

        mockMvc.perform(get("/api/v1/admin/community-posts/{postId}/comments", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("B001"));
    }

    @Test
    void 첨부파일_조회는_200과_목록을_반환한다() throws Exception {
        when(adminCommunityService.getAttachments(1L))
                .thenReturn(
                        List.of(
                                new AdminAttachmentResponse(
                                        3L,
                                        1L,
                                        "https://example.com/file.pdf",
                                        1024L,
                                        LocalDateTime.now())));

        mockMvc.perform(get("/api/v1/admin/community-posts/{postId}/attachments", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(3))
                .andExpect(jsonPath("$.data[0].fileKey").value("https://example.com/file.pdf"));
    }

    @Test
    void 게시글_삭제하면_200과_deletedAt이_채워진_게시글을_반환한다() throws Exception {
        AdminCommunityPostResponse deleted =
                new AdminCommunityPostResponse(
                        1L,
                        "제목",
                        PostCategory.QUESTION,
                        "내용",
                        10L,
                        "닉네임",
                        LocalDateTime.now(),
                        0,
                        LocalDateTime.now());
        when(adminCommunityService.deletePost(1L)).thenReturn(deleted);

        mockMvc.perform(delete("/api/v1/admin/community-posts/{postId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedAt").exists());
    }

    @Test
    void 게시글_삭제_대상이_없으면_404와_B001을_반환한다() throws Exception {
        when(adminCommunityService.deletePost(eq(1L)))
                .thenThrow(new BoardException(BoardErrorCode.POST_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/admin/community-posts/{postId}", 1L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("B001"));
    }
}
