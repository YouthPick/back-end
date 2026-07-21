package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.entity.PolicyApplicationChecklist;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyApplicationChecklistRepository
        extends JpaRepository<PolicyApplicationChecklist, Long> {

    /**
     * PolicyApplicationChecklistService.findActive(id, userId)가 이걸 감싼다 —
     * update/check/uncheck/delete 네 메서드가 모두 이 조회를 거친다.
     */
    Optional<PolicyApplicationChecklist> findByIdAndDeletedAtIsNull(Long id);

    /**
     * 체크리스트 노출 순서를 id 오름차순(등록 순서)으로 고정한다. PolicyApplicationChecklistService.getByApplication()에서 사용
     * — 목록 API의 실제 조회.
     */
    Page<PolicyApplicationChecklist> findByApplication_IdAndDeletedAtIsNullOrderByIdAsc(
            Long applicationId, Pageable pageable);

    /**
     * 이 리포지토리를 쓰는 다른 모든 메서드와 달리, 이건 PolicyApplicationChecklistService가 아니라 옆 도메인 파일인
     * PolicyApplicationService.create()의 reactivate 분기에서 직접 호출된다 — 사용자가 관심 삭제했던 정책을 재등록하면, 그 신청에
     * 달려있던 예전 체크리스트를 전부 무효화하기 위함이다. {@code flushAutomatically = true}라서, 직전에
     * PolicyApplication.reactivate()로 바뀐 변경분이 먼저 flush된 뒤에 이 벌크 UPDATE가 실행된다(순서 보장). {@code
     * clearAutomatically}는 안 켜져 있어서, 같은 트랜잭션 안에서 이미 영속 상태로 로드해둔 PolicyApplicationChecklist가 있다면 이
     * 벌크 UPDATE 이후에도 메모리 값은 갱신되지 않는다 — 지금 호출 지점(create())은 체크리스트 엔티티를 따로 로드하지 않아서 문제되지 않는다.
     */
    @Modifying(flushAutomatically = true)
    @Query(
            "UPDATE PolicyApplicationChecklist c SET c.deletedAt = CURRENT_TIMESTAMP "
                    + "WHERE c.application.id = :applicationId AND c.deletedAt IS NULL")
    void softDeleteAllByApplicationId(@Param("applicationId") Long applicationId);
}
