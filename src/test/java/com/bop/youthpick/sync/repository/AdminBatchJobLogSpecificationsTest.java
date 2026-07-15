package com.bop.youthpick.sync.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.sync.entity.BatchMode;
import com.bop.youthpick.sync.entity.BatchStatus;
import com.bop.youthpick.sync.entity.PolicyBatchHistory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
class AdminBatchJobLogSpecificationsTest {

    @Autowired private PolicyBatchHistoryRepository policyBatchHistoryRepository;

    @BeforeEach
    void setUp() {
        policyBatchHistoryRepository.save(
                newHistory(BatchStatus.SUCCEEDED, LocalDateTime.of(2026, 3, 15, 9, 0)));
        policyBatchHistoryRepository.save(
                newHistory(BatchStatus.FAILED, LocalDateTime.of(2026, 6, 1, 9, 0)));
        policyBatchHistoryRepository.save(
                newHistory(BatchStatus.SUCCEEDED, LocalDateTime.of(2026, 6, 10, 9, 0)));
    }

    private PolicyBatchHistory newHistory(BatchStatus status, LocalDateTime requestedAt) {
        PolicyBatchHistory history = BeanUtils.instantiateClass(PolicyBatchHistory.class);
        ReflectionTestUtils.setField(history, "mode", BatchMode.FULL);
        ReflectionTestUtils.setField(history, "status", status);
        ReflectionTestUtils.setField(history, "requestedAt", requestedAt);
        return history;
    }

    @Test
    void status로_필터링한다() {
        List<PolicyBatchHistory> result =
                policyBatchHistoryRepository.findAll(
                        AdminBatchJobLogSpecifications.filter("FAILED", null, null));

        assertThat(result)
                .extracting(PolicyBatchHistory::getStatus)
                .containsExactly(BatchStatus.FAILED);
    }

    @Test
    void 날짜범위로_필터링한다() {
        List<PolicyBatchHistory> result =
                policyBatchHistoryRepository.findAll(
                        AdminBatchJobLogSpecifications.filter(
                                null, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(BatchStatus.SUCCEEDED);
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<PolicyBatchHistory> result =
                policyBatchHistoryRepository.findAll(
                        AdminBatchJobLogSpecifications.filter(null, null, null));

        assertThat(result).hasSize(3);
    }
}
