package com.bop.youthpick.admin.board.controller;

import com.bop.youthpick.admin.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.admin.board.service.AdminCommunityService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "관리자 - 커뮤니티 댓글")
@RestController
@RequestMapping("/api/v1/admin/community-comments")
@RequiredArgsConstructor
public class AdminCommunityCommentController {

    private final AdminCommunityService adminCommunityService;

    @Operation(summary = "커뮤니티 댓글 삭제", description = "관리자가 커뮤니티 댓글을 삭제합니다.")
    @DeleteMapping("/{commentId}")
    public ApiResponse<AdminCommunityCommentResponse> delete(@PathVariable Long commentId) {
        return ApiResponse.ok(adminCommunityService.deleteComment(commentId));
    }
}
