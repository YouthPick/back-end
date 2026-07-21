package com.bop.youthpick.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PostUpdateRequest(
        @NotBlank(message = "카테고리는 필수입니다.")
                @Pattern(
                        regexp = "QUESTION|REVIEW|FREE",
                        message = "카테고리는 QUESTION, REVIEW, FREE 중 하나여야 합니다.")
                String category,
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
                String title,
        @NotBlank(message = "내용은 필수입니다.") String content,
        @Positive(message = "정책 ID는 양수여야 합니다.") Long policyId,
        @Size(max = 10, message = "이미지는 최대 10개까지 첨부할 수 있습니다.")
                List<
                                @Pattern(
                                        regexp =
                                                "/api/v1/files/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}",
                                        message = "업로드된 이미지 URL 형식이 아닙니다.")
                                String>
                        attachmentUrls) {}
