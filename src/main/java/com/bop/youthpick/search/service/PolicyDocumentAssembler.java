package com.bop.youthpick.search.service;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.search.dto.PolicyDocument;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 정책 엔티티를 색인 문서로 조립한다. 지역명은 {@code policy_regions} 조인이 필요해 엔티티만으로는 만들 수 없어서, 이 조회를 한곳에
 * 모았다 — 전체 재색인({@link PolicyReindexer})과 변경 반영({@link PolicySearchSyncListener})이 같은 규칙을 쓰게 하기
 * 위함이다.
 */
@Service
@RequiredArgsConstructor
public class PolicyDocumentAssembler {

    private final PolicyRegionRepository policyRegionRepository;

    /**
     * 정책들을 색인 문서로 바꾼다. 지역명을 정책마다 조회하면 N+1이 되므로 목록 전체의 지역을 fetch join 한 번으로 모아 정책 id별로
     * 묶는다 — {@code PolicyService.getCards}의 라벨 조립과 같은 방식이다.
     *
     * <p>삭제된 정책({@code deletedAt})은 검색에 나오면 안 되므로 애초에 문서를 만들지 않는다.
     */
    public List<PolicyDocument> assemble(List<Policy> policies) {
        if (policies.isEmpty()) {
            return List.of();
        }
        List<Long> policyIds = policies.stream().map(Policy::getId).toList();
        Map<Long, List<String>> sidoNamesByPolicyId =
                policyRegionRepository.findWithRegionByPolicyIdIn(policyIds).stream()
                        .collect(
                                Collectors.groupingBy(
                                        pr -> pr.getPolicy().getId(),
                                        Collectors.mapping(
                                                (PolicyRegion pr) -> pr.getRegion().getSidoName(),
                                                Collectors.toList())));

        return policies.stream()
                .filter(policy -> policy.getDeletedAt() == null)
                .map(policy -> PolicyDocument.from(policy, sidoNamesOf(sidoNamesByPolicyId, policy)))
                .toList();
    }

    private List<String> sidoNamesOf(Map<Long, List<String>> byPolicyId, Policy policy) {
        return byPolicyId.getOrDefault(policy.getId(), List.of()).stream()
                .distinct()
                .sorted()
                .toList();
    }
}
