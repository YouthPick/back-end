package com.bop.youthpick.sync.service;

import static com.bop.youthpick.policy.entity.PolicyFixture.policy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.repository.RegionRepository;
import com.bop.youthpick.sync.dto.YouthPolicyItem;
import com.bop.youthpick.sync.entity.BatchStatus;
import com.bop.youthpick.sync.entity.PolicyBatchHistory;
import com.bop.youthpick.sync.exception.PolicySyncException;
import com.bop.youthpick.sync.repository.PolicyBatchHistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Writer가 청크 커밋하는 흐름 전체를 검증하므로 테스트 트랜잭션을 끄고(@AfterEach 정리),
// 외부 API만 목으로 대체한 통합 테스트다 (배치 설계 §검증 — 성공/회차실패 시나리오).
@DataJpaTest
@Import({
    PolicySyncService.class,
    PolicyUpsertWriter.class,
    PolicyMapper.class,
    JpaAuditingConfig.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PolicySyncServiceTest {

    @TestConfiguration
    static class ObjectMapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private static final ObjectMapper OM = new ObjectMapper();

    @MockitoBean private PolicyApiClient policyApiClient;
    @MockitoBean private PolicySyncLock policySyncLock;

    // @DataJpaTest 슬라이스에는 Boot의 applicationTaskExecutor가 없다. 목으로 두면
    // 기본은 "실행 안 됨"이고, 필요한 테스트만 runInline()으로 현재 스레드 실행을 흉내낸다.
    @MockitoBean private org.springframework.core.task.TaskExecutor taskExecutor;

    @Autowired private PolicySyncService policySyncService;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private PolicyRegionRepository policyRegionRepository;
    @Autowired private RegionRepository regionRepository;
    @Autowired private PolicyBatchHistoryRepository historyRepository;

    @BeforeEach
    void acquireLockByDefault() {
        when(policySyncLock.tryAcquire()).thenReturn(Optional.of("test-token"));
    }

    @AfterEach
    void cleanUp() {
        policyRegionRepository.deleteAll();
        policyRepository.deleteAll();
        regionRepository.deleteAll();
        historyRepository.deleteAll();
    }

    @Test
    void 성공_회차는_신규_변경_SKIP_누락을_나눠_처리하고_이력에_기록한다() throws IOException {
        regionRepository.save(Region.create("11000", "서울특별시", "서울특별시"));
        policyRepository.save(policy("P-UPD", "옛 제목", LocalDateTime.of(2026, 1, 1, 0, 0)));
        policyRepository.save(policy("P-SAME", "그대로", LocalDateTime.of(2026, 1, 1, 0, 0)));
        policyRepository.save(policy("P-GONE", "사라질 정책", null));
        when(policyApiClient.fetchAll())
                .thenReturn(
                        List.of(
                                item(
                                        "{\"plcyNo\":\"P-NEW\",\"plcyNm\":\"신규 정책\","
                                                + "\"zipCd\":\"11000\"}"),
                                item(
                                        "{\"plcyNo\":\"P-UPD\",\"plcyNm\":\"갱신된 제목\","
                                                + "\"lastMdfcnDt\":\"2026-07-01 00:00:00\"}"),
                                item(
                                        "{\"plcyNo\":\"P-SAME\",\"plcyNm\":\"그대로\","
                                                + "\"lastMdfcnDt\":\"2026-01-01 00:00:00\"}")));

        PolicyBatchHistory result = policySyncService.runFullSync();

        assertThat(result.getStatus()).isEqualTo(BatchStatus.SUCCEEDED);
        assertThat(result.getNewCount()).isEqualTo(1);
        assertThat(result.getUpdatedCount()).isEqualTo(1);
        assertThat(result.getUnchangedCount()).isEqualTo(1);
        assertThat(result.getMissingCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isZero();
        assertThat(historyRepository.count()).isEqualTo(1); // 요청→실행→성공이 한 행에서 전이

        Policy created = policyRepository.findByPolicyNoIn(List.of("P-NEW")).getFirst();
        assertThat(created.getTitle()).isEqualTo("신규 정책");
        assertThat(policyRegionRepository.findAll())
                .singleElement()
                .satisfies(link -> assertThat(link.getRegion().getCode()).isEqualTo("11000"));

        Policy updated = policyRepository.findByPolicyNoIn(List.of("P-UPD")).getFirst();
        assertThat(updated.getTitle()).isEqualTo("갱신된 제목");

        Policy gone = policyRepository.findByPolicyNoIn(List.of("P-GONE")).getFirst();
        assertThat(gone.getMissingCount()).isEqualTo(1); // 첫 누락 — 아직 VISIBLE
        assertThat(gone.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
    }

    @Test
    void 전_시도를_커버하는_정책만_전국으로_표시한다() throws IOException {
        // 지역 마스터의 시도가 서울·부산 둘뿐인 상태 — 둘 다 걸린 정책이 '전국'이다.
        regionRepository.save(Region.create("11000", "서울특별시", "서울특별시"));
        regionRepository.save(Region.create("11680", "서울특별시", "강남구"));
        regionRepository.save(Region.create("26000", "부산광역시", "부산광역시"));
        when(policyApiClient.fetchAll())
                .thenReturn(
                        List.of(
                                item(
                                        "{\"plcyNo\":\"P-ALL\",\"plcyNm\":\"전국 정책\","
                                                + "\"zipCd\":\"11000,26000\"}"),
                                // 같은 시도 안에서 시군구만 여러 개 — 시도 수로 세므로 전국이 아니다.
                                item(
                                        "{\"plcyNo\":\"P-SEOUL\",\"plcyNm\":\"서울 정책\","
                                                + "\"zipCd\":\"11000,11680\"}"),
                                item("{\"plcyNo\":\"P-NONE\",\"plcyNm\":\"지역 없는 정책\"}")));

        policySyncService.runFullSync();

        assertThat(policyRepository.findByPolicyNoIn(List.of("P-ALL")).getFirst().isNationwide())
                .isTrue();
        assertThat(policyRepository.findByPolicyNoIn(List.of("P-SEOUL")).getFirst().isNationwide())
                .isFalse();
        assertThat(policyRepository.findByPolicyNoIn(List.of("P-NONE")).getFirst().isNationwide())
                .isFalse();
    }

    @Test
    void fetch가_실패하면_DB에_손대지_않고_FAILED_이력을_남긴_뒤_예외를_다시_던진다() {
        policyRepository.save(policy("P-KEEP", "건드리면 안 됨", null));
        when(policyApiClient.fetchAll())
                .thenThrow(new PolicySyncException("정책 API 1페이지 요청이 3회 모두 실패 — 회차 중단"));

        assertThatThrownBy(policySyncService::runFullSync).isInstanceOf(PolicySyncException.class);

        assertThat(historyRepository.findAll())
                .singleElement()
                .satisfies(
                        h -> {
                            assertThat(h.getStatus()).isEqualTo(BatchStatus.FAILED);
                            assertThat(h.getFailureMessage()).contains("회차 중단");
                        });
        Policy kept = policyRepository.findByPolicyNoIn(List.of("P-KEEP")).getFirst();
        assertThat(kept.getMissingCount()).isZero(); // 어제 데이터 그대로 — 누락 처리도 안 함
        assertThat(kept.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
        verify(policySyncLock).release("test-token"); // 실패해도 락은 반드시 해제
    }

    @Test
    void 락_획득에_실패하면_이력도_남기지_않고_아무_작업_없이_예외를_던진다() {
        when(policySyncLock.tryAcquire()).thenReturn(Optional.empty());

        assertThatThrownBy(policySyncService::runFullSync)
                .isInstanceOf(PolicySyncException.class)
                .hasMessageContaining("이미 실행 중");

        assertThat(historyRepository.count()).isZero(); // 거부된 시도는 회차가 아니다
        verify(policyApiClient, never()).fetchAll();
        verify(policySyncLock, never()).release(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void 숨겨진_정책이_같은_수정일로_재등장해도_변경으로_취급해_다시_보이게_한다() throws IOException {
        Policy hidden = policy("P-BACK", "돌아온 정책", LocalDateTime.of(2026, 1, 1, 0, 0));
        hidden.markMissing();
        hidden.markMissing();
        hidden.markMissing(); // HIDDEN
        policyRepository.save(hidden);
        when(policyApiClient.fetchAll())
                .thenReturn(
                        List.of(
                                item(
                                        "{\"plcyNo\":\"P-BACK\",\"plcyNm\":\"돌아온 정책\","
                                                + "\"lastMdfcnDt\":\"2026-01-01 00:00:00\"}")));

        PolicyBatchHistory result = policySyncService.runFullSync();

        assertThat(result.getUpdatedCount()).isEqualTo(1); // SKIP이 아니라 변경으로 분류돼야 복구된다
        Policy back = policyRepository.findByPolicyNoIn(List.of("P-BACK")).getFirst();
        assertThat(back.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
        assertThat(back.getMissingCount()).isZero();
    }

    @Test
    void 수동_실행은_락_거부시_409용_CustomException을_던지고_아무_작업도_하지_않는다() {
        when(policySyncLock.tryAcquire()).thenReturn(Optional.empty());

        assertThatThrownBy(policySyncService::startFullSyncAsync)
                .isInstanceOf(com.bop.youthpick.global.error.CustomException.class)
                .satisfies(
                        e ->
                                assertThat(
                                                ((com.bop.youthpick.global.error.CustomException) e)
                                                        .getErrorCode())
                                        .isEqualTo(
                                                com.bop.youthpick.sync.exception.SyncErrorCode
                                                        .SYNC_ALREADY_RUNNING));

        assertThat(historyRepository.count()).isZero();
        verify(taskExecutor, never()).execute(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void 수동_실행은_백그라운드에서_배치를_수행하고_락을_해제한다() {
        runInline();
        when(policyApiClient.fetchAll()).thenReturn(List.of());

        policySyncService.startFullSyncAsync();

        assertThat(historyRepository.findAll())
                .singleElement()
                .satisfies(h -> assertThat(h.getStatus()).isEqualTo(BatchStatus.SUCCEEDED));
        verify(policySyncLock).release("test-token");
    }

    @Test
    void 수동_실행의_백그라운드_실패는_밖으로_던지지_않고_FAILED_이력과_락_해제만_남긴다() {
        runInline();
        when(policyApiClient.fetchAll()).thenThrow(new PolicySyncException("페이지 요청 3회 실패"));

        policySyncService.startFullSyncAsync(); // 202는 이미 나간 뒤 — 예외가 여기로 나오면 안 된다

        assertThat(historyRepository.findAll())
                .singleElement()
                .satisfies(h -> assertThat(h.getStatus()).isEqualTo(BatchStatus.FAILED));
        verify(policySyncLock).release("test-token");
    }

    /** 목 executor가 제출된 작업을 현재 스레드에서 즉시 실행하게 한다. */
    private void runInline() {
        org.mockito.Mockito.doAnswer(
                        inv -> {
                            ((Runnable) inv.getArgument(0)).run();
                            return null;
                        })
                .when(taskExecutor)
                .execute(org.mockito.ArgumentMatchers.any(Runnable.class));
    }

    private static YouthPolicyItem item(String json) throws IOException {
        return OM.readValue(json, YouthPolicyItem.class);
    }
}
