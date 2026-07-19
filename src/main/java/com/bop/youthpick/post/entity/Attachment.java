package com.bop.youthpick.post.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 게시글 첨부파일. [미정] 첨부 대상이 게시글로 확정되지 않음 — 대상 바뀌면 FK만 교체. */
@Entity
@Table(
        name = "attachments",
        indexes = @Index(name = "idx_attachments_post", columnList = "post_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    /** byte */
    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
