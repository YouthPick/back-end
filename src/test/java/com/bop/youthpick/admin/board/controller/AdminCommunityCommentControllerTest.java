package com.bop.youthpick.admin.board.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.bop.youthpick.admin.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.admin.board.service.AdminCommunityService;
import com.bop.youthpick.board.exception.BoardErrorCode;
import com.bop.youthpick.board.exception.BoardException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = AdminCommunityCommentController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminCommunityCommentControllerTest {

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AdminCommunityService adminCommunityService;

    @Test
    void 댓글_삭제하면_200과_deletedAt이_채워진_댓글을_반환한다() throws Exception {
        AdminCommunityCommentResponse deleted =
                new AdminCommunityCommentResponse(
                        2L, 1L, null, "닉네임", "댓글", LocalDateTime.now(), LocalDateTime.now());
        when(adminCommunityService.deleteComment(2L)).thenReturn(deleted);

        mockMvc.perform(delete("/api/v1/admin/community-comments/{commentId}", 2L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.deletedAt").exists());
    }

    @Test
    void 삭제_대상이_없으면_404와_B002를_반환한다() throws Exception {
        when(adminCommunityService.deleteComment(2L))
                .thenThrow(new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));

        mockMvc.perform(delete("/api/v1/admin/community-comments/{commentId}", 2L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("B002"));
    }
}
