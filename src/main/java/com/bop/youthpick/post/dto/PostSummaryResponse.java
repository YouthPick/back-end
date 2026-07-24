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
        String contentExcerpt,
        int viewCount,
        LocalDateTime createdAt) {

    // 목록 카드 미리보기용으로 본문을 잘라서 내려준다. 태그가 잘려도 프론트는 텍스트만
    // 추출해 보여주므로(dangerouslySetInnerHTML로 렌더하지 않음) 문제되지 않는다.
    private static final int CONTENT_EXCERPT_MAX_LENGTH = 300;

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
                truncate(post.getContent()),
                post.getViewCount(),
                post.getCreatedAt());
    }

    private static String truncate(String content) {
        if (content == null) {
            return "";
        }
        return content.length() > CONTENT_EXCERPT_MAX_LENGTH
                ? content.substring(0, CONTENT_EXCERPT_MAX_LENGTH)
                : content;
    }
}
