package com.bop.youthpick.sync.service;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.policy.repository.PolicyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PolicySyncStartupRunnerTest {

    @Mock private PolicyRepository policyRepository;

    @Mock private PolicySyncService policySyncService;

    @InjectMocks private PolicySyncStartupRunner runner;

    @Test
    @DisplayName("policies가 비어 있으면 시작 시 전량 수집을 실행한다")
    void runsInitialSyncWhenEmpty() {
        when(policyRepository.count()).thenReturn(0L);

        runner.run(null);

        verify(policySyncService).runFullSync();
    }

    @Test
    @DisplayName("policies에 데이터가 있으면 수집을 실행하지 않는다")
    void skipsWhenPoliciesExist() {
        when(policyRepository.count()).thenReturn(2633L);

        runner.run(null);

        verify(policySyncService, never()).runFullSync();
    }
}
