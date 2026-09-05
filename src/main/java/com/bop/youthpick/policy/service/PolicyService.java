package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.log.service.SearchLogService;
import com.bop.youthpick.policy.dto.PolicyCardResponse;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.search.config.PolicySearchProperties;
import com.bop.youthpick.search.dto.PolicySearchQuery;
import com.bop.youthpick.search.dto.PolicySearchResult;
import com.bop.youthpick.search.service.PolicySearchService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 대상 정책 조회. 관리자용 조회/수정은 {@link AdminPolicyService}. */
@Service
@RequiredArgsConstructor
public class PolicyService {

    private static final Logger log = LoggerFactory.getLogger(PolicyService.class);

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final PolicyRecentViewService policyRecentViewService;
    private final SearchLogService searchLogService;
    private final PolicySearchService policySearchService;
    private final PolicySearchProperties searchProperties;

    private static final String NATIONWIDE_REGION = "전국";

    /**
     * 정책 목록(카드) 조회 (비회원 허용). 삭제·숨김·신청 마감 지난 정책은 제외한다. category(표준 5분류) exact match, keyword는 5개 필드
     * LIKE 부분일치, region은 시도명 필터('전국'은 지역 무관이라 필터 미적용), age는 [ageMin, ageMax] 구간과 정책 자격 구간의
     * 겹침(overlap), jobCode는 온통청년 취업상태 코드 필터다. 지역 라벨은 페이지 단위 배치 조회(fetch join)로 조립해 N+1을 피한다.
     *
     * <p>정렬은 리포지토리 쿼리가 고정한다({@code PolicyRepository.findCards} 참고) — 지역·취업상태 필터가 걸렸을 때만 '조건 없는 정책'을
     * 뒤로 미뤄야 해서 파라미터에 따라 순서가 달라지기 때문이다. 여기서는 정렬 없는 Pageable을 넘긴다.
     */
    @Transactional(readOnly = true)
    public Page<PolicyCardResponse> getCards(
            @Nullable String category,
            @Nullable String keyword,
            @Nullable String region,
            @Nullable Integer ageMin,
            @Nullable Integer ageMax,
            @Nullable String jobCode,
            Pageable pageable) {
        if (searchProperties.enabled()) {
            try {
                return searchViaElasticsearch(
                        category, keyword, region, ageMin, ageMax, jobCode, pageable);
            } catch (Exception e) {
                // 검색 품질은 떨어지지만 아무것도 못 보여주는 것보다는 낫다. 조용히 넘기지 않고 로그를 남긴다 —
                // 폴백이 계속 도는 상황을 아무도 모르는 것이 진짜 사고다.
                log.warn("ES 검색 실패 — MySQL 경로로 폴백합니다", e);
            }
        }
        return searchViaMysql(category, keyword, region, ageMin, ageMax, jobCode, pageable);
    }

    /**
     * ES 로 걸리는 정책 id 를 받고, 카드 내용은 MySQL 에서 읽는다. 색인이 조금 늦어도 화면에 보이는 값은 항상 원본 기준이고, 응답 조립 코드도
     * 폴백 경로와 그대로 공유된다.
     *
     * <p>ES 가 돌려준 순서(관련도)가 곧 응답 순서다. {@code findAllById} 는 순서를 보장하지 않으므로 id 순서에 맞춰 다시 세운다.
     */
    private Page<PolicyCardResponse> searchViaElasticsearch(
            @Nullable String category,
            @Nullable String keyword,
            @Nullable String region,
            @Nullable Integer ageMin,
            @Nullable Integer ageMax,
            @Nullable String jobCode,
            Pageable pageable)
            throws Exception {
        String searchKeyword = trimToNull(keyword);
        String regionFilter = trimToNull(region);
        PolicySearchResult result =
                policySearchService.search(
                        new PolicySearchQuery(
                                searchKeyword,
                                trimToNull(category),
                                NATIONWIDE_REGION.equals(regionFilter) ? null : regionFilter,
                                ageMin,
                                ageMax,
                                trimToNull(jobCode),
                                LocalDate.now(),
                                pageable.getPageNumber(),
                                pageable.getPageSize()));

        if (searchKeyword != null) {
            searchLogService.record(searchKeyword, (int) result.total());
        }

        Map<Long, Policy> policiesById =
                policyRepository
                        .findAllByIdInAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                                result.policyIds(), PolicyVisibility.VISIBLE)
                        .stream()
                        .collect(Collectors.toMap(Policy::getId, policy -> policy));
        // 색인과 원본이 어긋난 사이(삭제 직후 등)에는 id 가 비어 있을 수 있다 — 그 건만 조용히 빠진다.
        List<Policy> policies =
                result.policyIds().stream().map(policiesById::get).filter(Objects::nonNull).toList();

        return new PageImpl<>(toCards(policies), pageable, result.total());
    }

    private Page<PolicyCardResponse> searchViaMysql(
            @Nullable String category,
            @Nullable String keyword,
            @Nullable String region,
            @Nullable Integer ageMin,
            @Nullable Integer ageMax,
            @Nullable String jobCode,
            Pageable pageable) {
        String categoryFilter = trimToNull(category);
        String keywordPattern = toLikePattern(keyword);
        String regionFilter = trimToNull(region);
        // '전국'은 지역 드롭다운의 "지역 무관" 선택지 — 시도명 필터를 걸지 않는다.
        // 전국 대상 정책은 policy_regions에 전 시도가 들어 있어 개별 시도 조회에도 이미 포함된다.
        String sidoName = NATIONWIDE_REGION.equals(regionFilter) ? null : regionFilter;

        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());

        Page<Policy> page =
                policyRepository.findCards(
                        PolicyVisibility.VISIBLE,
                        LocalDate.now(),
                        categoryFilter,
                        keywordPattern,
                        sidoName,
                        ageMin,
                        ageMax,
                        trimToNull(jobCode),
                        unsorted);
        if (keywordPattern != null) {
            searchLogService.record(trimToNull(keyword), (int) page.getTotalElements());
        }

        return new PageImpl<>(toCards(page.getContent()), pageable, page.getTotalElements());
    }

    /** 정책들을 카드 응답으로 조립한다. 지역 라벨은 페이지 단위 배치 조회(fetch join)로 N+1 을 피한다. */
    private List<PolicyCardResponse> toCards(List<Policy> policies) {
        List<Long> policyIds = policies.stream().map(Policy::getId).toList();
        Map<Long, List<String>> sidoNamesByPolicyId =
                policyIds.isEmpty()
                        ? Map.of()
                        : policyRegionRepository.findWithRegionByPolicyIdIn(policyIds).stream()
                                .collect(
                                        Collectors.groupingBy(
                                                pr -> pr.getPolicy().getId(),
                                                Collectors.mapping(
                                                        pr -> pr.getRegion().getSidoName(),
                                                        Collectors.toList())));
        return policies.stream()
                .map(
                        policy ->
                                PolicyCardResponse.from(
                                        policy,
                                        sidoNamesByPolicyId
                                                .getOrDefault(policy.getId(), List.of())
                                                .stream()
                                                .distinct()
                                                .sorted()
                                                .toList()))
                .toList();
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** LIKE 특수문자(%/_)와 이스케이프 문자(!)를 이스케이프하고 부분일치 패턴으로 감싼다. */
    @Nullable
    private static String toLikePattern(@Nullable String keyword) {
        String normalized = trimToNull(keyword);
        if (normalized == null) {
            return null;
        }
        String escaped = normalized.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    /**
     * 정책 상세 조회. 삭제(soft delete)·숨김 정책은 존재하지 않는 것으로 취급한다. 로그인 사용자({@code userId != null})의 조회는 최근 본
     * 정책으로 기록하되, 기록은 부가 동작이라 실패해도 조회는 정상 응답한다.
     */
    @Transactional(readOnly = true)
    public PolicyDetailResponse getDetail(Long policyId, @Nullable Long userId) {
        Policy policy =
                policyRepository
                        .findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                                policyId, PolicyVisibility.VISIBLE)
                        .orElseThrow(() -> new CustomException(PolicyErrorCode.POLICY_NOT_FOUND));

        if (userId != null) {
            try {
                policyRecentViewService.record(userId, policy);
            } catch (RuntimeException e) {
                log.warn(
                        "최근 본 정책 기록에 실패했습니다. 조회는 정상 진행합니다. userId={}, policyId={}",
                        userId,
                        policyId,
                        e);
            }
        }

        List<RegionResponse> regions =
                policyRegionRepository.findWithRegionByPolicyIdIn(List.of(policyId)).stream()
                        .map(policyRegion -> RegionResponse.from(policyRegion.getRegion()))
                        .toList();
        return PolicyDetailResponse.from(policy, regions);
    }
}
