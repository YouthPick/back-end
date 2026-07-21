package com.bop.youthpick.policy.entity;

import com.bop.youthpick.global.entity.BaseEntity;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 신청관리별 준비 체크리스트 (제출서류 등). PolicyApplicationChecklistService를 통해서만 생성/변경되고, application
 * FK(policy_application_checklists.application_id)로 부모 {@link PolicyApplication}에 종속된다 — 부모가 소프트
 * 삭제되면 이 엔티티는 개별적으로는 안 지워지고 남아있는 채로 "고아" 상태가 되는데, 그건
 * PolicyApplicationChecklistService.findActive()가 checklist.getApplication().isDeleted()로 걸러낸다.
 */
@Entity
@Table(
        name = "policy_application_checklists",
        indexes =
                @Index(
                        name = "idx_policy_application_checklists_app",
                        columnList = "application_id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PolicyApplicationChecklist extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false)
    private PolicyApplication application;

    /**
     * 컬럼/필드명은 content이지만, 이걸 감싸는 DTO 쪽(PolicyApplicationCreateChecklistRequest 등)은 전부 message라는 이름을
     * 쓴다 — 같은 값인데 계층마다 이름이 달라서 코드를 오갈 때 헷갈리기 쉽다.
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * Lombok {@code @Getter}가 boolean 필드 checked에 대해 {@code isChecked()}를 생성한다 —
     * PolicyApplicationChecklistResponse.from()이 이 getter로 값을 읽는다.
     */
    @Column(name = "is_checked", nullable = false)
    private boolean checked;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    /**
     * PolicyApplicationChecklistService.add()에서만 호출되는 정적 팩토리 — 새 항목은 항상 미체크(checked=false) 상태로
     * 시작한다.
     */
    public static PolicyApplicationChecklist create(PolicyApplication application, String content) {
        PolicyApplicationChecklist checklist = new PolicyApplicationChecklist();
        checklist.application = application;
        checklist.content = content;
        checklist.checked = false;
        return checklist;
    }

    /** PolicyApplicationChecklistService.update()에서 호출. */
    public void updateContent(String content) {
        this.content = content;
    }

    /** PolicyApplicationChecklistService.check()에서 호출. */
    public void check() {
        this.checked = true;
    }

    /** PolicyApplicationChecklistService.uncheck()에서 호출. */
    public void uncheck() {
        this.checked = false;
    }

    /**
     * PolicyApplicationChecklistService.delete()에서 호출하는 개별 소프트 삭제. 여러 항목을 한 번에 지우는 경로(부모 신청관리 재등록
     * 시)는 이 메서드를 쓰지 않고 PolicyApplicationChecklistRepository.softDeleteAllByApplicationId()가 벌크
     * UPDATE로 처리한다.
     */
    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }
}
