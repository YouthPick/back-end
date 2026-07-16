package com.bop.youthpick.board.dto;

import com.bop.youthpick.post.entity.Post;
import com.bop.youthpick.post.entity.PostCategory;
import java.time.LocalDateTime;

public record AdminCommunityPostResponse(
        Long id,
        String title,
        PostCategory category,
        String content,
        Long authorId,
        String authorName,
        LocalDateTime createdAt,
        int viewCount,
        LocalDateTime deletedAt) {
    public static AdminCommunityPostResponse from(Post post) {
        return new AdminCommunityPostResponse(
                post.getId(),
                post.getTitle(),
                post.getCategory(),
                post.getContent(),
                post.getUser().getId(),
                post.getUser().getNickname(),
                post.getCreatedAt(),
                post.getViewCount(),
                post.getDeletedAt());
    }
}
