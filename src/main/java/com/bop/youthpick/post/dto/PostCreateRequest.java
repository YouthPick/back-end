package com.bop.youthpick.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PostCreateRequest(
        @NotBlank(message = "카테고리는 필수입니다.")
                @Pattern(
                        regexp = "QUESTION|REVIEW|FREE",
                        message = "카테고리는 QUESTION, REVIEW, FREE 중 하나여야 합니다.")
                String category,
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
                String title,
        @NotBlank(message = "내용은 필수입니다.") String content,
        @Positive(message = "정책 ID는 양수여야 합니다.") Long policyId) {}
