package com.bop.youthpick.sync.service;

import com.bop.youthpick.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 앱 시작 시 policies가 비어 있으면 초기 수집을 1회 실행한다. 팀원 누구나 compose up + 앱 실행만으로 실데이터를 갖게 하는 장치.
 *
 * <p>youthpick.sync.startup-on-empty=true 인 프로파일에서만 동작한다 (기본 off — 테스트/CI의 빈 H2에서 외부 API를 치는 사고
 * 방지).
 */
@Component
@ConditionalOnProperty(name = "youthpick.sync.startup-on-empty", havingValue = "true")
@RequiredArgsConstructor
public class PolicySyncStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PolicySyncStartupRunner.class);

    private final PolicyRepository policyRepository;
    private final PolicySyncService policySyncService;

    // ponytail: 동기 실행 — 초기 수집(~수십 초)이 앱 기동을 막는다.
    // 기동 시간이 문제되면 @Async 또는 별도 스레드로 전환.
    @Override
    public void run(ApplicationArguments args) {
        long count = policyRepository.count();
        if (count > 0) {
            log.info("policies {}건 존재 — 시작 시 초기 수집 생략", count);
            return;
        }
        log.info("policies 비어 있음 — 초기 수집을 시작합니다");
        try {
            policySyncService.runFullSync();
        } catch (RuntimeException e) {
            log.warn("초기 수집 실패 — 외부 API 혹은 DB 장애가 의심됩니다. 수동 실행을 수행하거나 확인하세요.", e);
        }
    }
}
