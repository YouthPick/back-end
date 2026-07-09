package com.bop.youthpick.policy.entity;

import com.bop.youthpick.global.entity.BaseEntity;
import com.bop.youthpick.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 정책 신청 진행관리 (관심 → 신청 → 완료). 기존 favorite_policies를 흡수 — status=INTERESTED가 즐겨찾기. UNIQUE(user,
 * policy)로 같은 정책 중복 등록 방지.
 */
@Entity
@Table(
        name = "policy_applications",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_policy_applications_user_policy",
                        columnNames = {"user_id", "policy_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ApplicationStatus status;

    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 개인이 설정하는 마감일 */
    @Column(name = "end_at")
    private LocalDateTime endAt;

    /** 관리 해제 = soft delete. 재등록 시 행 재활성화(UNIQUE 충돌 방지) */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static PolicyApplication register(
            User user, Policy policy, ApplicationStatus status, String memo, LocalDateTime endAt) {
        PolicyApplication application = new PolicyApplication();
        application.user = user;
        application.policy = policy;
        application.status = status;
        application.memo = memo;
        application.endAt = endAt;
        return application;
    }

    public void changeStatus(ApplicationStatus status) {
        this.status = status;
    }

    /** soft-delete된 행을 UNIQUE(user, policy) 충돌 없이 재등록한다. */
    public void reactivate(ApplicationStatus status, String memo, LocalDateTime endAt) {
        this.status = status;
        this.memo = memo;
        this.endAt = endAt;
        this.deletedAt = null;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
