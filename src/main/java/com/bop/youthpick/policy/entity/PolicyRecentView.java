package com.bop.youthpick.policy.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import com.bop.youthpick.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 최근 본 정책 — 로그인 사용자가 정책 상세를 조회한 기록. 같은 정책을 다시 보면 행을 늘리지 않고 {@link #touch()}로 viewed_at만 갱신한다
 * (UNIQUE(user, policy)).
 */
@Entity
@Table(
        name = "policy_recent_views",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_policy_recent_views_user_policy",
                        columnNames = {"user_id", "policy_id"}),
        indexes =
                @Index(
                        name = "idx_policy_recent_views_user_viewed",
                        columnList = "user_id, viewed_at"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyRecentView extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    /** 마지막 조회 시각 (재조회 시 갱신) */
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;

    public static PolicyRecentView create(User user, Policy policy) {
        PolicyRecentView view = new PolicyRecentView();
        view.user = user;
        view.policy = policy;
        view.viewedAt = LocalDateTime.now();
        return view;
    }

    public void touch() {
        this.viewedAt = LocalDateTime.now();
    }
}
