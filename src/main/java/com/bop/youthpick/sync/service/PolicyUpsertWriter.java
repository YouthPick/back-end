package com.bop.youthpick.sync.service;

import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.sync.dto.PolicyUpsertItem;
import com.bop.youthpick.sync.dto.PolicyWriteResult;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public PolicyWriteResult upsertAll(List<PolicyUpsertItem> items) {
        int newCount = 0;
        int updatedCount = 0;
        int errorCount = 0;
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
                                status ->
                                        writeOne(
                                                freshItem,
                                                policyRepository
                                                        .findByPolicyNoIn(List.of(policyNo))
                                                        .stream()
                                                        .findFirst()
                                                        .orElse(null)));
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
        int newCount = 0;
        int updatedCount = 0;
        for (PolicyUpsertItem item : chunk) {
            if (writeOne(item, existingByNo.get(item.policy().getPolicyNo()))) {
                newCount++;
            } else {
                updatedCount++;
            }
        }
        return new ChunkResult(newCount, updatedCount, 0);
    }

    /** 1건 저장. 신규면 true, 변경이면 false. */
    private boolean writeOne(PolicyUpsertItem item, Policy existing) {
        Policy target;
        boolean isNew = existing == null;
        if (isNew) {
            target = policyRepository.save(item.policy());
        } else {
            existing.updateFrom(item.policy());
            // Hibernate는 flush 시 INSERT를 DELETE보다 먼저 실행하므로, 같은 지역이 유지되는
            // 정책에서 (policy_id, region_code) 유니크 충돌이 난다 — 삭제를 먼저 flush한다.
            policyRegionRepository.deleteByPolicy(existing);
            policyRegionRepository.flush();
            target = existing;
        }
        for (var region : item.regions()) {
            policyRegionRepository.save(PolicyRegion.create(target, region));
        }
        // 지역 행과 같은 트랜잭션에서 갱신한다 — 따로 두면 둘이 어긋나 정렬이 조용히 틀린다(#120).
        target.applyRegionCoverage(item.nationwide());
        return isNew;
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
                                return found.size();
                            });
        }
        return marked;
    }
}
