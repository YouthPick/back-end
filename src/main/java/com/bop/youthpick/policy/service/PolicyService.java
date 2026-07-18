package com.bop.youthpick.policy.service;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyCardResponse;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.repository.RegionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final RegionRepository regionRepository;
    private final PolicyRecentViewService policyRecentViewService;

    /**
     * 정책 목록(카드) 조회 (비회원 허용). 삭제·숨김·신청 마감 지난 정책은 제외하고 최신순(id 내림차순, 서버 고정 정렬)으로 내린다. category(표준
     * 5분류)를 주면 해당 분류만. 지역 라벨은 페이지 단위 배치 조회(fetch join)로 조립해 N+1을 피한다.
     */
    @Transactional(readOnly = true)
    public Page<PolicyCardResponse> getCards(@Nullable String category, Pageable pageable) {
        Pageable sorted =
                PageRequest.of(
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        Sort.by(Sort.Direction.DESC, "id"));
        String categoryFilter = category == null || category.isBlank() ? null : category;
        Page<Policy> page =
                policyRepository.findCards(
                        PolicyVisibility.VISIBLE, LocalDate.now(), categoryFilter, sorted);

        List<Long> policyIds = page.getContent().stream().map(Policy::getId).toList();
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
        long totalSidoCount = policyIds.isEmpty() ? 0 : regionRepository.countDistinctSidoNames();

        return page.map(
                policy ->
                        PolicyCardResponse.from(
                                policy,
                                toRegionLabel(
                                        sidoNamesByPolicyId.getOrDefault(policy.getId(), List.of()),
                                        totalSidoCount)));
    }

    /** 지역 없음 → null, 전 시도 커버 → '전국', 시도 1개 → 시도명, 여러 시도 → '가나다 첫 시도 외 N'. */
    @Nullable
    private static String toRegionLabel(List<String> sidoNames, long totalSidoCount) {
        List<String> distinct = sidoNames.stream().distinct().sorted().toList();
        if (distinct.isEmpty()) {
            return null;
        }
        if (totalSidoCount > 0 && distinct.size() >= totalSidoCount) {
            return "전국";
        }
        if (distinct.size() == 1) {
            return distinct.get(0);
        }
        return distinct.get(0) + " 외 " + (distinct.size() - 1);
    }

    /**
     * 정책 상세 조회. 삭제(soft delete)·숨김 정책은 존재하지 않는 것으로 취급한다. 로그인 사용자({@code userId != null})의 조회는 최근 본
     * 정책으로 기록하되, 기록은 부가 동작이라 실패해도 조회는 정상 응답한다.
     */
    @Transactional(readOnly = true)
    public PolicyDetailResponse getDetail(Long policyId, @Nullable Long userId) {
        Policy policy =
                policyRepository
                        .findByIdAndVisibilityAndDeletedAtIsNull(policyId, PolicyVisibility.VISIBLE)
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
                policyRegionRepository.findByPolicyIdIn(List.of(policyId)).stream()
                        .map(policyRegion -> RegionResponse.from(policyRegion.getRegion()))
                        .toList();
        return PolicyDetailResponse.from(policy, regions);
    }
}
