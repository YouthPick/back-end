package com.bop.youthpick.admin.board.dto;

import com.bop.youthpick.comment.entity.Comment;
import java.time.LocalDateTime;

public record AdminCommunityCommentResponse(
        Long id,
        Long postId,
        Long parentCommentId,
        String authorName,
        String content,
        LocalDateTime createdAt,
        LocalDateTime deletedAt) {
    public static AdminCommunityCommentResponse from(Comment comment) {
        return new AdminCommunityCommentResponse(
                comment.getId(),
                comment.getPost().getId(),
                comment.getParent() == null ? null : comment.getParent().getId(),
                comment.getUser().getNickname(),
                comment.getContent(),
                comment.getCreatedAt(),
                comment.getDeletedAt());
    }
}
