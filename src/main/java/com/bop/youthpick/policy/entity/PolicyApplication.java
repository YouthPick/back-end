package com.bop.youthpick.policy.entity;

import com.bop.youthpick.auth.exception.AuthErrorCode;
import com.bop.youthpick.auth.exception.AuthException;
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
import org.hibernate.annotations.SQLRestriction;

/**
 * 사용자별 정책 신청 진행관리 (관심 → 신청 → 완료). 기존 favorite_policies를 흡수 — status=INTERESTED가 즐겨찾기. UNIQUE(user,
 * policy)로 같은 정책 중복 등록 방지. 이 엔티티는 오직 PolicyApplicationService를 통해서만 생성/변경된다 — Controller에서 직접 다루지
 * 않는다(entity-jpa.md 규칙). 조회는 PolicyApplicationRepository, API 노출은 PolicyApplicationResponse.from()이
 * 담당한다.
 */
@Entity
@Table(
        name = "policy_applications",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_policy_applications_user_policy",
                        columnNames = {"user_id", "policy_id"}))
@SQLRestriction("deleted_at IS NULL")
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

    /**
     * 정적 팩토리 — {@link com.bop.youthpick.policy.service.PolicyApplicationService#create}의 "완전히 새로운
     * 신청" 경로에서만 호출된다(기존에 soft-delete된 행이 있으면 이걸 쓰지 않고 {@link #reactivate}로 같은 행을 재사용한다).
     */
    public static PolicyApplication create(
            User user, Policy policy, ApplicationStatus status, String memo, LocalDateTime endAt) {
        PolicyApplication application = new PolicyApplication();
        application.user = user;
        application.policy = policy;
        application.status = status;
        application.memo = memo;
        application.endAt = endAt;
        return application;
    }

    /**
     * setter 대신 의미 있는 메서드로 상태를 변경한다(entity-jpa.md 규칙). {@code save()} 호출 없이 필드만 바꿔도
     * {@code @Transactional} 커밋 시 JPA dirty checking으로 자동 UPDATE된다.
     */
    public void changeStatus(ApplicationStatus status) {
        this.status = status;
    }

    /** null 정규화(공백/빈 문자열 → null)는 서비스 계층 책임이라 여기선 그대로 대입만 한다. */
    public void updateMemo(String memo) {
        this.memo = memo;
    }

    /** 정책 마감일 초과 검증은 서비스 책임이고, 엔티티는 검증된 값을 그대로 받아 대입만 한다. */
    public void updateEndAt(LocalDateTime endAt) {
        this.endAt = endAt;
    }

    /**
     * soft-delete된 행을 UNIQUE(user, policy) 충돌 없이 재등록한다. PolicyApplicationService.create()의 "기존 신청이
     * 있는데 삭제된 상태" 분기에서만 호출되며, status/memo/endAt을 과거 값과 무관하게 이번 요청 값으로 완전히 덮어쓴다. 리뷰 노트: 이
     * 엔티티엔 @Version(낙관적 락)이 없어서, 같은 (user, policy)에 대해 동시에 재등록 요청이 들어오면 나중에 커밋되는 쪽이 조용히 덮어쓸 수 있다 —
     * create()의 "새 INSERT" 경로가 DataIntegrityViolationException으로 명시적으로 막는 것과 달리 이 경로는 방어가 없다.
     */
    public void reactivate(ApplicationStatus status, String memo, LocalDateTime endAt) {
        this.status = status;
        this.memo = memo;
        this.endAt = endAt;
        this.deletedAt = null;
    }

    /**
     * 물리 DELETE가 아니라 {@code deletedAt} 타임스탬프만 세팅하는 소프트 삭제다. UNIQUE(user, policy) 제약은 유지한 채로 "삭제됨"만
     * 표시해서, 나중에 {@link #reactivate}가 이 행을 재사용할 수 있게 한다.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    /** {@link #create}가 "새로 만들지 vs 재활성화할지"를 가르는 조건이자, {@code findActive()}류 조회 필터의 기준이 된다. */
    public boolean isDeleted() {
        return deletedAt != null;
    }

    /**
     * 소유권 검증 — {@code PolicyApplicationService}(changeStatus/updateMemo/updateEndAt/delete)와 {@code
     * PolicyApplicationChecklistService}가 공통으로 사용한다. 원래 두 서비스에 토씨 하나 안 다른 private verifyOwner()가 각각
     * 중복돼 있었는데, "이 신청의 소유자가 누구인가"는 이 엔티티 자신이 답할 수 있는 질문이라 엔티티 메서드로 옮겼다.
     */
    public void verifyOwner(Long userId) {
        if (!user.getId().equals(userId)) {
            throw new AuthException(AuthErrorCode.FORBIDDEN);
        }
    }
}
