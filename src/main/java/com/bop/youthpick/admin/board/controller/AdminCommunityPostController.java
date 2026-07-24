package com.bop.youthpick.admin.board.controller;

import com.bop.youthpick.admin.board.dto.AdminAttachmentResponse;
import com.bop.youthpick.admin.board.dto.AdminCommunityCommentResponse;
import com.bop.youthpick.admin.board.dto.AdminCommunityPostResponse;
import com.bop.youthpick.admin.board.service.AdminCommunityService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "관리자 - 커뮤니티 게시글")
@RestController
@RequestMapping("/api/v1/admin/community-posts")
@RequiredArgsConstructor
@Validated
public class AdminCommunityPostController {

    private final AdminCommunityService adminCommunityService;

    @Operation(
            summary = "커뮤니티 게시글 목록 조회",
            description = "카테고리, 작성자, 작성일 범위로 커뮤니티 게시글을 검색해 페이지 단위로 조회합니다.")
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

    @Operation(summary = "게시글 댓글 목록 조회", description = "지정한 게시글에 달린 댓글 목록을 조회합니다.")
    @GetMapping("/{postId}/comments")
    public ApiResponse<List<AdminCommunityCommentResponse>> getComments(@PathVariable Long postId) {
        return ApiResponse.ok(adminCommunityService.getComments(postId));
    }

    @Operation(summary = "게시글 첨부파일 목록 조회", description = "지정한 게시글에 첨부된 파일 목록을 조회합니다.")
    @GetMapping("/{postId}/attachments")
    public ApiResponse<List<AdminAttachmentResponse>> getAttachments(@PathVariable Long postId) {
        return ApiResponse.ok(adminCommunityService.getAttachments(postId));
    }

    @Operation(summary = "커뮤니티 게시글 삭제", description = "관리자가 커뮤니티 게시글을 삭제합니다.")
    @DeleteMapping("/{postId}")
    public ApiResponse<AdminCommunityPostResponse> delete(@PathVariable Long postId) {
        return ApiResponse.ok(adminCommunityService.deletePost(postId));
    }
}
