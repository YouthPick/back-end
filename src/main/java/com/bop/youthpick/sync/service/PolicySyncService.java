package com.bop.youthpick.sync.service;

import com.bop.youthpick.global.error.CustomException;
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
import com.bop.youthpick.sync.exception.PolicySyncException;
import com.bop.youthpick.sync.exception.SyncErrorCode;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

/** 온통청년 정책 전량 수집 배치. 상세 흐름: docs/2026-06-29-데이터수집-배치-설계.md (팀 루트 저장소) */
@Slf4j
@Service
@RequiredArgsConstructor
public class PolicySyncService {

    private final PolicySyncLock policySyncLock;
    private final TaskExecutor taskExecutor;
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
     *
     * <p>중복 실행은 Redis 락으로 거부한다. 거부된 시도는 회차가 아니므로 이력을 남기지 않는다.
     */
    public PolicyBatchHistory runFullSync() {
        String lockToken =
                policySyncLock
                        .tryAcquire()
                        .orElseThrow(() -> new PolicySyncException("정책 수집이 이미 실행 중 — 중복 실행 거부"));
        try {
            return doRunFullSync();
        } finally {
            policySyncLock.release(lockToken);
        }
    }

    /**
     * 관리자 수동 실행. 락은 이 스레드에서 동기로 획득해 즉시 202/409를 판정할 수 있게 하고, 수십 초짜리 실행 본체는 백그라운드로 넘긴다(HTTP 응답이 배치를
     * 기다리면 타임아웃). 백그라운드 실패는 이력(FAILED)에 이미 남으므로 로그만 남긴다 — 202가 나간 뒤라 받을 사람도 없다.
     */
    public void startFullSyncAsync() {
        String lockToken =
                policySyncLock
                        .tryAcquire()
                        .orElseThrow(() -> new CustomException(SyncErrorCode.SYNC_ALREADY_RUNNING));
        taskExecutor.execute(
                () -> {
                    try {
                        doRunFullSync();
                    } catch (RuntimeException e) {
                        log.error("정책 수집 수동 실행 실패 — 상세는 policy_batch_history 참조", e);
                    } finally {
                        policySyncLock.release(lockToken);
                    }
                });
    }

    private PolicyBatchHistory doRunFullSync() {
        PolicyBatchHistory history =
                historyRepository.save(PolicyBatchHistory.request(BatchMode.FULL));
        history.start();
        history = historyRepository.save(history);
        try {
            List<YouthPolicyItem> items = policyApiClient.fetchAll();
            SyncPlan plan = plan(items);
            PolicyWriteResult writeResult = policyUpsertWriter.upsertAll(plan.upserts());
            int missingMarked = policyUpsertWriter.hideMissing(plan.missingPolicyNos());

            int totalProcessed = plan.upserts().size();
            double errorRate = 0.0;
            if (totalProcessed > 0) {
                errorRate = (double) writeResult.errorCount() / totalProcessed;
            }

            if (errorRate > 0.1) {
                String errorMsg = "정책 수집 실패율 10% 초과 — 에러 건수: %d/%d (%.2f%%)"
                        .formatted(writeResult.errorCount(), totalProcessed, errorRate * 100);
                history.fail(errorMsg);
                log.error("정책 수집 완료되었으나 실패율 기준 초과로 작업 실패 처리함: {}", errorMsg);
            } else {
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
            }
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
        // 전 시도 커버 판정 기준(#120). 이미 로드한 지역 마스터에서 세므로 추가 쿼리가 없다.
        long totalSidoCount =
                regionsByCode.values().stream().map(Region::getSidoName).distinct().count();

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
                    && snapshot.lastModifiedAt() != null
                    && policy.getLastModifiedAt() != null
                    && Objects.equals(snapshot.lastModifiedAt(), policy.getLastModifiedAt())) {
                unchangedCount++;
                continue;
            }
            List<Region> regions =
                    policyMapper.resolveRegions(policyNo, item.zipCd(), regionsByCode);
            upserts.add(
                    new PolicyUpsertItem(policy, regions, isNationwide(regions, totalSidoCount)));
        }

        // API 응답에 아예 안 나타난 정책만 누락 — SKIP(unchanged)된 정책은 존재하는 것
        List<String> missingPolicyNos =
                snapshots.keySet().stream().filter(no -> !seenPolicyNos.contains(no)).toList();
        return new SyncPlan(upserts, missingPolicyNos, unchangedCount);
    }

    /**
     * 전 시도를 커버하면 전국 정책(#120). 온통청년 zipCd는 지역 한정 정책에도 전국 코드를 담는 경우가 있어, 조회 시 지역 특화 정책을 먼저 노출하려면 이
     * 구분이 필요하다. 지역 수가 아니라 시도 수로 판정한다 — 시군구 개수는 시도마다 달라 임계값을 정할 수 없기 때문이다.
     */
    private static boolean isNationwide(List<Region> regions, long totalSidoCount) {
        if (totalSidoCount <= 0 || regions.isEmpty()) {
            return false;
        }
        return regions.stream().map(Region::getSidoName).distinct().count() >= totalSidoCount;
    }

    // 앞으로 Insert/Delete할 정책들(신규+변경), 앞으로 누락 처리 할 정책번호들, 아무것도 안할 건수(이력 기록용)
    private record SyncPlan(
            List<PolicyUpsertItem> upserts, List<String> missingPolicyNos, int unchangedCount) {}
}
