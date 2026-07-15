package com.bop.youthpick.post.dto;

import com.bop.youthpick.post.entity.Post;
import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        Long authorId,
        String authorNickname,
        Long policyId,
        String policyTitle,
        String category,
        String title,
        int viewCount,
        LocalDateTime createdAt) {

    public static PostSummaryResponse from(Post post) {
        Long policyId = post.getPolicy() == null ? null : post.getPolicy().getId();
        String policyTitle = post.getPolicy() == null ? null : post.getPolicy().getTitle();
        return new PostSummaryResponse(
                post.getId(),
                post.getUser().getId(),
                post.getUser().getNickname(),
                policyId,
                policyTitle,
                post.getCategory().name(),
                post.getTitle(),
                post.getViewCount(),
                post.getCreatedAt());
    }
}
