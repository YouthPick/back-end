package com.bop.youthpick.comment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentUpdateRequest(
        @NotBlank(message = "댓글 내용은 필수입니다.") @Size(max = 400, message = "댓글은 400자 이하여야 합니다.")
                String content) {}
