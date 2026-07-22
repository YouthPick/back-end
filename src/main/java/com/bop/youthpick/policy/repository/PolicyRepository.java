package com.bop.youthpick.policy.repository;

import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PolicyRepository
        extends JpaRepository<Policy, Long>, JpaSpecificationExecutor<Policy> {

    /** 배치 비교용 전체 스냅샷 — HIDDEN 포함 (제외 이유는 {@link PolicySyncSnapshot} 참고). */
    @Query(
            "select new com.bop.youthpick.policy.dto.PolicySyncSnapshot("
                    + "p.policyNo, p.lastModifiedAt, p.visibility) from Policy p")
    List<PolicySyncSnapshot> findSyncSnapshots();

    List<Policy> findByPolicyNoIn(Collection<String> policyNos);

    long countByVisibilityAndDeletedAtIsNull(PolicyVisibility visibility);

    Optional<Policy> findByIdAndVisibilityAndDeletedAtIsNull(Long id, PolicyVisibility visibility);

    /**
     * 목록 카드 조회 — 노출 중이고 신청 마감(applicationEndDate)이 지나지 않은 정책만. 마감일 없음(상시)은 포함하되,
     * businessPeriodEnd(사업기간 종료일)가 있고 이미 지났다면 제외한다 — aplyPrdSeCd가 진짜 상시(0057002)가 아닌데도
     * aplyYmd(신청기간)가 비어 applicationEndDate만 null인 정책(예: 0057003 지역 단발성 모집)이 이미 끝났음에도 "상시"로 계속 노출되는
     * 문제를 막는다. category는 표준 5분류(V8에서 정규화) exact match, null이면 전체. keyword는 5개 필드 LIKE 부분일치(escape
     * '!'). sidoName은 시도명 EXISTS(같은 시도 내 다수 시군구여도 중복 반환 없음) — 전 시도를 커버하는 정책도 개별 시도 조회에 포함된다. age는
     * 요청 구간과 정책 자격 구간의 겹침(overlap) 판정 — min/maxAge가 0 또는 NULL이면 제한없음으로 항상 통과.
     */
    @Query(
            "select p from Policy p where p.visibility = :visibility and p.deletedAt is null"
                    + " and (p.applicationEndDate is null or p.applicationEndDate >= :today)"
                    + " and (p.applicationEndDate is not null"
                    + "     or p.businessPeriodEnd is null or p.businessPeriodEnd >= :today)"
                    + " and (:category is null or p.category = :category)"
                    + " and (:keyword is null"
                    + "     or p.title like :keyword escape '!'"
                    + "     or p.keywords like :keyword escape '!'"
                    + "     or p.description like :keyword escape '!'"
                    + "     or p.supportContent like :keyword escape '!'"
                    + "     or p.organizationName like :keyword escape '!')"
                    + " and (:sidoName is null or exists ("
                    + "     select pr from PolicyRegion pr"
                    + "     where pr.policy = p and pr.region.sidoName = :sidoName))"
                    + " and (:ageMin is null or p.maxAge is null or p.maxAge = 0 or p.maxAge >= :ageMin)"
                    + " and (:ageMax is null or p.minAge is null or p.minAge = 0 or p.minAge <= :ageMax)")
    Page<Policy> findCards(
            @Param("visibility") PolicyVisibility visibility,
            @Param("today") LocalDate today,
            @Param("category") String category,
            @Param("keyword") String keyword,
            @Param("sidoName") String sidoName,
            @Param("ageMin") Integer ageMin,
            @Param("ageMax") Integer ageMax,
            Pageable pageable);
}
