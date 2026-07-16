package com.bop.youthpick.board.controller;

import com.bop.youthpick.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.board.service.AdminCommunityService;
import com.bop.youthpick.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/community-comments")
@RequiredArgsConstructor
public class AdminCommunityCommentController {

    private final AdminCommunityService adminCommunityService;

    @DeleteMapping("/{commentId}")
    public ApiResponse<AdminCommunityCommentResponse> delete(@PathVariable Long commentId) {
        return ApiResponse.ok(adminCommunityService.deleteComment(commentId));
    }
}
