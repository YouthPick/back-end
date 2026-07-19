package com.bop.youthpick.admin.board.dto;

import com.bop.youthpick.post.entity.Attachment;
import java.time.LocalDateTime;

public record AdminAttachmentResponse(
        Long id, Long postId, String fileKey, Long fileSize, LocalDateTime createdAt) {
    public static AdminAttachmentResponse from(Attachment attachment) {
        return new AdminAttachmentResponse(
                attachment.getId(),
                attachment.getPost().getId(),
                attachment.getFileUrl(),
                attachment.getFileSize(),
                attachment.getCreatedAt());
    }
}
