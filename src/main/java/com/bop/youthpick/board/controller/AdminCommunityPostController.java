package com.bop.youthpick.board.controller;

import com.bop.youthpick.board.dto.AdminAttachmentResponse;
import com.bop.youthpick.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.board.dto.AdminCommunityPostResponse;
import com.bop.youthpick.board.service.AdminCommunityService;
import com.bop.youthpick.global.common.ApiResponse;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/community/posts")
@RequiredArgsConstructor
@Validated
public class AdminCommunityPostController {

    private final AdminCommunityService adminCommunityService;

    @GetMapping
    public ApiResponse<List<AdminCommunityPostResponse>> list(
            @RequestParam(required = false)
                    @Pattern(
                            regexp = "QUESTION|REVIEW|FREE",
                            message = "category는 QUESTION, REVIEW, FREE 중 하나여야 합니다.")
                    String category,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate endDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<AdminCommunityPostResponse> page =
                adminCommunityService.searchPosts(category, authorId, startDate, endDate, pageable);
        return ApiResponse.ok(page.getContent(), page);
    }

    @GetMapping("/{postId}/comments")
    public ApiResponse<List<AdminCommunityCommentResponse>> getComments(@PathVariable Long postId) {
        return ApiResponse.ok(adminCommunityService.getComments(postId));
    }

    @GetMapping("/{postId}/attachments")
    public ApiResponse<List<AdminAttachmentResponse>> getAttachments(@PathVariable Long postId) {
        return ApiResponse.ok(adminCommunityService.getAttachments(postId));
    }

    @DeleteMapping("/{postId}")
    public ApiResponse<AdminCommunityPostResponse> delete(@PathVariable Long postId) {
        return ApiResponse.ok(adminCommunityService.deletePost(postId));
    }
}
