package com.bop.youthpick.sync.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.bop.youthpick.sync.exception.PolicySyncException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class PolicySyncSchedulerTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner()
                    .withBean(PolicySyncService.class, () -> mock(PolicySyncService.class))
                    .withUserConfiguration(PolicySyncScheduler.class);

    @Test
    void 설정이_켜져_있으면_스케줄러_빈이_등록된다() {
        runner.withPropertyValues(
                        "youthpick.sync.scheduler.enabled=true",
                        "youthpick.sync.scheduler.cron=0 0 4 * * *")
                .run(context -> assertThat(context).hasSingleBean(PolicySyncScheduler.class));
    }

    @Test
    void 설정이_없거나_꺼져_있으면_스케줄러_빈이_등록되지_않는다() {
        runner.run(context -> assertThat(context).doesNotHaveBean(PolicySyncScheduler.class));
        runner.withPropertyValues("youthpick.sync.scheduler.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(PolicySyncScheduler.class));
    }

    @Test
    void 배치가_실패해도_예외를_밖으로_던지지_않는다() {
        PolicySyncService service = mock(PolicySyncService.class);
        when(service.runFullSync()).thenThrow(new PolicySyncException("정책 수집이 이미 실행 중"));

        PolicySyncScheduler scheduler = new PolicySyncScheduler(service);

        assertThatCode(scheduler::runDailySync).doesNotThrowAnyException();
    }
}
