package com.bop.youthpick.sync.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PolicyBatchHistoryTest {

    @Test
    void request로_생성하면_REQUESTED_상태와_요청시각이_기록된다() {
        PolicyBatchHistory history = PolicyBatchHistory.request(BatchMode.FULL);

        assertThat(history.getMode()).isEqualTo(BatchMode.FULL);
        assertThat(history.getStatus()).isEqualTo(BatchStatus.REQUESTED);
        assertThat(history.getRequestedAt()).isNotNull();
        assertThat(history.getStartedAt()).isNull();
        assertThat(history.getFinishedAt()).isNull();
    }

    @Test
    void start하면_RUNNING_상태와_시작시각이_기록된다() {
        PolicyBatchHistory history = PolicyBatchHistory.request(BatchMode.FULL);

        history.start();

        assertThat(history.getStatus()).isEqualTo(BatchStatus.RUNNING);
        assertThat(history.getStartedAt()).isNotNull();
    }

    @Test
    void succeed하면_SUCCEEDED_상태와_종료시각_카운트가_기록된다() {
        PolicyBatchHistory history = PolicyBatchHistory.request(BatchMode.FULL);
        history.start();

        history.succeed(10, 5, 2000, 3, 1);

        assertThat(history.getStatus()).isEqualTo(BatchStatus.SUCCEEDED);
        assertThat(history.getFinishedAt()).isNotNull();
        assertThat(history.getNewCount()).isEqualTo(10);
        assertThat(history.getUpdatedCount()).isEqualTo(5);
        assertThat(history.getUnchangedCount()).isEqualTo(2000);
        assertThat(history.getMissingCount()).isEqualTo(3);
        assertThat(history.getErrorCount()).isEqualTo(1);
    }

    @Test
    void fail하면_FAILED_상태와_실패_메시지가_기록된다() {
        PolicyBatchHistory history = PolicyBatchHistory.request(BatchMode.FULL);
        history.start();

        history.fail("정책 API 3페이지 요청이 3회 모두 실패 — 회차 중단");

        assertThat(history.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(history.getFinishedAt()).isNotNull();
        assertThat(history.getFailureMessage()).contains("회차 중단");
    }

    @Test
    void 실패_메시지가_컬럼_한도를_넘으면_잘라서_기록한다() {
        PolicyBatchHistory history = PolicyBatchHistory.request(BatchMode.FULL);

        history.fail("가".repeat(2000));

        assertThat(history.getFailureMessage()).hasSize(1000);
    }
}
