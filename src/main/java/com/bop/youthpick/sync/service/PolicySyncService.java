package com.bop.youthpick.sync.service;

import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.repository.RegionRepository;
import com.bop.youthpick.sync.dto.PolicyUpsertItem;
import com.bop.youthpick.sync.dto.PolicyWriteResult;
import com.bop.youthpick.sync.dto.YouthPolicyItem;
import com.bop.youthpick.sync.entity.BatchMode;
import com.bop.youthpick.sync.entity.PolicyBatchHistory;
import com.bop.youthpick.sync.repository.PolicyBatchHistoryRepository;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/** 온통청년 정책 전량 수집 배치. 상세 흐름: docs/2026-06-29-데이터수집-배치-설계.md (팀 루트 저장소) */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySyncService {

    private final PolicyApiClient policyApiClient;
    private final PolicyMapper policyMapper;
    private final PolicyUpsertWriter policyUpsertWriter;
    private final PolicyRepository policyRepository;
    private final RegionRepository regionRepository;
    private final PolicyBatchHistoryRepository historyRepository;

    /**
     * 전량 수집 1회 실행: 이력 REQUESTED→RUNNING → 전량 fetch → 스냅샷 비교(신규/변경/SKIP) → 청크 upsert → 누락 처리 → 이력
     * SUCCEEDED. fetch가 실패하면 DB에 손대지 않고 FAILED 기록 후 예외를 다시 던진다.
     *
     * <p>이 메서드 자체는 트랜잭션이 아니다 — 커밋 단위는 Writer의 청크와 이력 save 각각이다.
     */
    public PolicyBatchHistory runFullSync() {
        PolicyBatchHistory history =
                historyRepository.save(PolicyBatchHistory.request(BatchMode.FULL));
        history.start();
        history = historyRepository.save(history);
        try {
            List<YouthPolicyItem> items = policyApiClient.fetchAll();
            SyncPlan plan = plan(items);
            PolicyWriteResult writeResult = policyUpsertWriter.upsertAll(plan.upserts());
            int missingMarked = policyUpsertWriter.hideMissing(plan.missingPolicyNos());
            history.succeed(
                    writeResult.newCount(),
                    writeResult.updatedCount(),
                    plan.unchangedCount(),
                    missingMarked,
                    writeResult.errorCount());
            log.info(
                    "정책 수집 완료 — 신규 {} / 변경 {} / 유지 {} / 누락 {} / 실패 {}",
                    writeResult.newCount(),
                    writeResult.updatedCount(),
                    plan.unchangedCount(),
                    missingMarked,
                    writeResult.errorCount());
            return historyRepository.save(history);
        } catch (RuntimeException e) {
            history.fail(e.getMessage());
            historyRepository.save(history);
            throw e;
        }
    }

    /** 스냅샷(1쿼리)·지역 마스터(1쿼리)를 로드해 메모리에서 비교 분류한다. 없음=신규 / lastMdfcnDt 다름=변경 / 같음=SKIP. */
    private SyncPlan plan(List<YouthPolicyItem> items) {
        Map<String, PolicySyncSnapshot> snapshots =
                policyRepository.findSyncSnapshots().stream()
                        .collect(
                                Collectors.toMap(
                                        PolicySyncSnapshot::policyNo, Function.identity()));
        Map<String, Region> regionsByCode =
                regionRepository.findAll().stream()
                        .collect(Collectors.toMap(Region::getCode, Function.identity()));

        // 수집 도중 페이지 경계가 밀리면 같은 정책이 두 번 올 수 있다 — plcyNo 기준으로 접는다(먼저 온 것 유지)
        Set<String> seenPolicyNos = new HashSet<>();
        List<PolicyUpsertItem> upserts = new ArrayList<>();
        int unchangedCount = 0;
        for (YouthPolicyItem item : items) {
            Policy policy = policyMapper.toEntity(item);
            String policyNo = policy.getPolicyNo();
            if (policyNo == null) {
                log.warn("plcyNo 없는 응답 항목 — 스킵");
                continue;
            }
            if (!seenPolicyNos.add(policyNo)) {
                continue;
            }
            PolicySyncSnapshot snapshot = snapshots.get(policyNo);
            // SKIP은 "수정일 동일 + 현재 보이는 정책"만. HIDDEN이 같은 수정일로 재등장하면
            // 변경으로 취급해 updateFrom이 VISIBLE 복구 + missing_count 리셋을 수행하게 한다.
            if (snapshot != null
                    && snapshot.visibility() == PolicyVisibility.VISIBLE
                    && Objects.equals(snapshot.lastModifiedAt(), policy.getLastModifiedAt())) {
                unchangedCount++;
                continue;
            }
            List<Region> regions =
                    policyMapper.resolveRegions(policyNo, item.zipCd(), regionsByCode);
            upserts.add(new PolicyUpsertItem(policy, regions));
        }

        // API 응답에 아예 안 나타난 정책만 누락 — SKIP(unchanged)된 정책은 존재하는 것
        List<String> missingPolicyNos =
                snapshots.keySet().stream().filter(no -> !seenPolicyNos.contains(no)).toList();
        return new SyncPlan(upserts, missingPolicyNos, unchangedCount);
    }

    private record SyncPlan(
            List<PolicyUpsertItem> upserts, List<String> missingPolicyNos, int unchangedCount) {}
}
