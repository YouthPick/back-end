package com.bop.youthpick.sync.service;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.search.dto.PolicyChangedEvent;
import com.bop.youthpick.sync.dto.PolicyUpsertItem;
import com.bop.youthpick.sync.dto.PolicyWriteResult;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/** 전처리된 정책을 청크 단위 트랜잭션으로 저장(신규 INSERT / 변경 UPDATE)하고, 사라진 정책을 숨김 처리한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class PolicyUpsertWriter {

    static final int CHUNK_SIZE = 100;

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final TransactionTemplate transactionTemplate;
    // 커밋된 뒤 검색 색인을 갱신하기 위한 이벤트 발행(PolicySearchSyncListener 가 받는다).
    private final ApplicationEventPublisher eventPublisher;

    public PolicyWriteResult upsertAll(List<PolicyUpsertItem> items) {
        int newCount = 0;
        int updatedCount = 0;
        int errorCount = 0;
        int processed = 0;
        try {
            for (int from = 0; from < items.size(); from += CHUNK_SIZE) {
                List<PolicyUpsertItem> chunk =
                        items.subList(from, Math.min(from + CHUNK_SIZE, items.size()));
                ChunkResult result;
                try {
                    result = transactionTemplate.execute(status -> writeChunk(chunk));
                } catch (RuntimeException e) {
                    // 청크에 깨진 건이 섞임 — 건별 재시도로 범인만 제외하고 나머지는 살린다.
                    log.warn("정책 청크 저장 실패({}건) — 건별 재시도", chunk.size(), e);
                    result = retryOneByOne(chunk);
                }
                newCount += result.newCount();
                updatedCount += result.updatedCount();
                errorCount += result.errorCount();
                processed += chunk.size();
            }
        } catch (RuntimeException e) {
            // 건별 재시도까지 뚫고 올라온 예외 = DB 장애급. 앞선 청크는 이미 커밋됐는데 이력에는
            // 카운트 0으로 남으므로(PolicySyncService의 catch), 어디까지 갔는지는 여기서만 알 수 있다.
            // ponytail: 로그만 — 실제로 발생하면 부분 결과를 예외에 실어 이력까지 옮긴다.
            log.error(
                    "정책 저장 중단 — {}/{}건 처리 후 실패 (신규 {} / 변경 {} / 건별실패 {})",
                    processed,
                    items.size(),
                    newCount,
                    updatedCount,
                    errorCount);
            throw e;
        }
        return new PolicyWriteResult(newCount, updatedCount, errorCount);
    }

    private ChunkResult retryOneByOne(List<PolicyUpsertItem> chunk) {
        int newCount = 0;
        int updatedCount = 0;
        int errorCount = 0;
        for (PolicyUpsertItem item : chunk) {
            String policyNo = item.policy().getPolicyNo();
            // 실패한 청크에서 IDENTITY가 id를 부여한 채 롤백된 엔티티일 수 있어 재사용 금지 — 새 복사본으로 저장
            PolicyUpsertItem freshItem =
                    new PolicyUpsertItem(
                            Policy.copyOf(item.policy()), item.regions(), item.nationwide());
            try {
                boolean isNew =
                        transactionTemplate.execute(
                                status -> {
                                    Policy existing =
                                            policyRepository
                                                    .findByPolicyNoIn(List.of(policyNo))
                                                    .stream()
                                                    .findFirst()
                                                    .orElse(null);
                                    Policy saved =
                                            writeOne(
                                                    freshItem,
                                                    existing,
                                                    regionCodesByPolicyId(
                                                            existing == null
                                                                    ? List.of()
                                                                    : List.of(existing)));
                                    publishChanged(List.of(saved.getId()));
                                    return existing == null;
                                });
                if (isNew) {
                    newCount++;
                } else {
                    updatedCount++;
                }
            } catch (RuntimeException e) {
                errorCount++;
                log.warn("정책 {} 저장 실패 — 이번 회차 스킵", policyNo, e);
            }
        }
        return new ChunkResult(newCount, updatedCount, errorCount);
    }

    private ChunkResult writeChunk(List<PolicyUpsertItem> chunk) {
        Map<String, Policy> existingByNo =
                policyRepository
                        .findByPolicyNoIn(
                                chunk.stream().map(i -> i.policy().getPolicyNo()).toList())
                        .stream()
                        .collect(Collectors.toMap(Policy::getPolicyNo, p -> p));
        Map<Long, Set<String>> regionCodesByPolicyId = regionCodesByPolicyId(existingByNo.values());
        int newCount = 0;
        int updatedCount = 0;
        List<Long> savedIds = new ArrayList<>();
        for (PolicyUpsertItem item : chunk) {
            Policy existing = existingByNo.get(item.policy().getPolicyNo());
            savedIds.add(writeOne(item, existing, regionCodesByPolicyId).getId());
            if (existing == null) {
                newCount++;
            } else {
                updatedCount++;
            }
        }
        publishChanged(savedIds);
        return new ChunkResult(newCount, updatedCount, 0);
    }

    /**
     * 기존 정책들이 현재 갖고 있는 지역 코드 집합 (policyId → codes). 쿼리 1회로 청크 전체를 떠서 {@link #writeOne}이 지역 변경 여부를
     * 판정하는 데 쓴다.
     */
    private Map<Long, Set<String>> regionCodesByPolicyId(Collection<Policy> existingPolicies) {
        List<Long> policyIds = existingPolicies.stream().map(Policy::getId).toList();
        if (policyIds.isEmpty()) {
            return Map.of();
        }
        return policyRegionRepository.findWithRegionByPolicyIdIn(policyIds).stream()
                .collect(
                        Collectors.groupingBy(
                                policyRegion -> policyRegion.getPolicy().getId(),
                                Collectors.mapping(
                                        policyRegion -> policyRegion.getRegion().getCode(),
                                        Collectors.toSet())));
    }

    /** 1건 저장. 색인 갱신에 쓰도록 저장된 엔티티를 돌려준다(신규 여부는 호출부가 existing 으로 판단). */
    private Policy writeOne(
            PolicyUpsertItem item, Policy existing, Map<Long, Set<String>> regionCodesByPolicyId) {
        Policy target;
        if (existing == null) {
            target = policyRepository.save(item.policy());
            saveRegions(target, item.regions());
        } else {
            existing.updateFrom(item.policy());
            target = existing;
            // 지역 집합이 그대로면 손대지 않는다 — 정책 본문만 바뀐 회차에서 전국 정책 1건당
            // DELETE 256 + INSERT 256이 그대로 나가던 것을 없앤다.
            Set<String> currentCodes =
                    regionCodesByPolicyId.getOrDefault(existing.getId(), Set.of());
            Set<String> nextCodes =
                    item.regions().stream().map(Region::getCode).collect(Collectors.toSet());
            if (!currentCodes.equals(nextCodes)) {
                // Hibernate는 flush 시 INSERT를 DELETE보다 먼저 실행하므로, 같은 지역이 유지되는
                // 정책에서 (policy_id, region_code) 유니크 충돌이 난다 — 삭제를 먼저 flush한다.
                policyRegionRepository.deleteByPolicy(existing);
                policyRegionRepository.flush();
                saveRegions(target, item.regions());
            }
        }
        // 지역 행과 같은 트랜잭션에서 갱신한다 — 따로 두면 둘이 어긋나 정렬이 조용히 틀린다(#120).
        target.applyRegionCoverage(item.nationwide());
        return target;
    }

    private void saveRegions(Policy policy, List<Region> regions) {
        for (Region region : regions) {
            policyRegionRepository.save(PolicyRegion.create(policy, region));
        }
    }

    /**
     * 저장된 정책들의 색인 갱신을 요청한다. 트랜잭션 <b>안에서</b> 발행해야 리스너가 커밋 직후에 받는다 — 커밋 전에 색인하면 롤백 시 ES 에만
     * 남고, 커밋 밖에서 발행하면 AFTER_COMMIT 리스너가 아예 호출되지 않는다.
     *
     * <p>건별이 아니라 청크 단위로 묶어 던져, 색인 요청이 정책 건수만큼 늘어나지 않게 한다.
     */
    private void publishChanged(List<Long> policyIds) {
        if (!policyIds.isEmpty()) {
            eventPublisher.publishEvent(PolicyChangedEvent.updated(policyIds));
        }
    }

    private record ChunkResult(int newCount, int updatedCount, int errorCount) {}

    /** 이번 수집에서 사라진 정책의 누락 횟수를 올린다(3회 도달 시 HIDDEN — Policy.markMissing). DB에 없는 번호는 무시. */
    public int hideMissing(List<String> missingPolicyNos) {
        int marked = 0;
        for (int from = 0; from < missingPolicyNos.size(); from += CHUNK_SIZE) {
            List<String> chunk =
                    missingPolicyNos.subList(
                            from, Math.min(from + CHUNK_SIZE, missingPolicyNos.size()));
            marked +=
                    transactionTemplate.execute(
                            status -> {
                                List<Policy> found = policyRepository.findByPolicyNoIn(chunk);
                                found.forEach(Policy::markMissing);
                                // 누락 3회로 HIDDEN이 되면 검색에서 빠져야 하므로 색인도 갱신한다.
                                publishChanged(found.stream().map(Policy::getId).toList());
                                return found.size();
                            });
        }
        return marked;
    }
}
