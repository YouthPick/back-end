package com.bop.youthpick.sync.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정책 전량 수집을 매일 1회 실행하는 스케줄러. 로컬 기본 off — 팀원이 앱을 켤 때마다 실 API를 호출하는 사고 방지
 * (`youthpick.sync.scheduler.enabled`).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "youthpick.sync.scheduler.enabled", havingValue = "true")
public class PolicySyncScheduler {

    private final PolicySyncService policySyncService;

    /** 실패해도 예외를 던지지 않는다 — 결과는 policy_batch_history(FAILED)에 이미 남고, 다음 스케줄은 계속 돌아야 한다. */
    @Scheduled(cron = "${youthpick.sync.scheduler.cron}")
    public void runDailySync() {
        try {
            policySyncService.runFullSync();
        } catch (RuntimeException e) {
            log.error("정책 수집 스케줄 실행 실패 — 상세는 policy_batch_history 참조", e);
        }
    }
}
