package com.bop.youthpick.comment.controller;

import com.bop.youthpick.auth.service.CurrentUser;
import com.bop.youthpick.comment.dto.CommentCreateRequest;
import com.bop.youthpick.comment.dto.CommentResponse;
import com.bop.youthpick.comment.dto.CommentUpdateRequest;
import com.bop.youthpick.comment.service.CommentService;
import com.bop.youthpick.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "댓글")
@RestController
@RequestMapping("/api/v1/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "댓글 등록", description = "특정 게시글에 새로운 댓글을 작성하여 등록한다.")
    @PostMapping
    public ResponseEntity<ApiResponse<CommentResponse>> create(
            @CurrentUser Long userId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentCreateRequest request) {
        CommentResponse res = commentService.create(userId, postId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(res));
    }

    @Operation(summary = "댓글 목록 조회", description = "특정 게시글에 달린 댓글 전체 목록을 조회한다.")
    @GetMapping
    public ApiResponse<List<CommentResponse>> findAll(
            @PathVariable Long postId, @CurrentUser(required = false) Long userId) {
        return commentService.getAllByPostId(postId);
    }

    @Operation(summary = "댓글 수정", description = "댓글 ID로 대상 댓글의 내용을 수정한다.")
    @PatchMapping("/{commentId}")
    public ApiResponse<CommentResponse> update(
            @CurrentUser Long userId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @Valid @RequestBody CommentUpdateRequest request) {
        return ApiResponse.ok(commentService.update(userId, commentId, request));
    }

    @Operation(
            summary = "댓글 삭제",
            description = "댓글 ID로 대상 댓글을 삭제한다. 작성자 본인만 삭제할 수 있는지는 Service에서 검사한다.")
    @DeleteMapping("/{commentId}")
    public ApiResponse<Void> delete(
            // [추가] @CurrentUser: JWT 토큰에서 로그인한 사용자의 id를 꺼내 주입해 준다.
            // 작성자 본인만 삭제할 수 있는지 Service에서 검사해야 하므로 함께 넘긴다.
            @CurrentUser Long userId, @PathVariable Long postId, @PathVariable Long commentId) {
        commentService.delete(userId, commentId);
        return ApiResponse.ok(null);
    }
}
