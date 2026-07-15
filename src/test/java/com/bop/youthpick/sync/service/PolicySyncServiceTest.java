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

    private static YouthPolicyItem item(String json) throws IOException {
        return OM.readValue(json, YouthPolicyItem.class);
    }
}
