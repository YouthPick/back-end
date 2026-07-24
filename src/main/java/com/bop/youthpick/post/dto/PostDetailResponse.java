package com.bop.youthpick.post.dto;

import com.bop.youthpick.post.entity.Post;
import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        Long authorId,
        String authorNickname,
        Long policyId,
        String policyTitle,
        String category,
        String title,
        String content,
        int viewCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static PostDetailResponse from(Post post) {
        Long policyId = post.getPolicy() == null ? null : post.getPolicy().getId();
        String policyTitle = post.getPolicy() == null ? null : post.getPolicy().getTitle();
        return new PostDetailResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getNickname(),
                policyId,
                policyTitle,
                post.getCategory().name(),
                post.getTitle(),
                post.getContent(),
                post.getViewCount(),
                post.getCreatedAt(),
                post.getUpdatedAt());
    }
}
