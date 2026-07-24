package com.bop.youthpick.post.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.user.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 정책 기반 게시판 글. policy가 NULL이면 자유글. 정책 행을 재활용하지 않고 FK로 참조만 — 정책(배치가 주인)과 게시글(유저가 주인)은 수명이 다르므로. */
@Entity
@Table(name = "posts", indexes = @Index(name = "idx_posts_policy", columnList = "policy_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private PostCategory category;

    @Column(length = 100, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static Post create(
            User user, Policy policy, PostCategory category, String title, String content) {
        Post post = new Post();
        post.user = user;
        post.policy = policy;
        post.category = category;
        post.title = title;
        post.content = content;
        return post;
    }

    public void update(Policy policy, PostCategory category, String title, String content) {
        this.policy = policy;
        this.category = category;
        this.title = title;
        this.content = content;
    }

    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void incrementViewCount() {
        this.viewCount += 1;
    }
}
