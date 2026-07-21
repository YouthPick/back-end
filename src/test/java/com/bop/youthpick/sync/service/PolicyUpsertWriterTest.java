package com.bop.youthpick.sync.service;

import static com.bop.youthpick.policy.entity.PolicyFixture.policy;
import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.repository.RegionRepository;
import com.bop.youthpick.sync.dto.PolicyUpsertItem;
import com.bop.youthpick.sync.dto.PolicyWriteResult;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

// Writer가 청크마다 자체 트랜잭션을 커밋하는 컴포넌트라, 테스트를 감싸는 트랜잭션(기본 롤백)과
// 충돌하지 않도록 NOT_SUPPORTED로 끄고 @AfterEach에서 직접 정리한다.
@DataJpaTest
@Import({PolicyUpsertWriter.class, JpaAuditingConfig.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PolicyUpsertWriterTest {

    @Autowired private PolicyUpsertWriter writer;
    @Autowired private PolicyRepository policyRepository;
    @Autowired private PolicyRegionRepository policyRegionRepository;
    @Autowired private RegionRepository regionRepository;

    @AfterEach
    void cleanUp() {
        policyRegionRepository.deleteAll();
        policyRepository.deleteAll();
        regionRepository.deleteAll();
    }

    @Test
    void 신규_정책을_INSERT하고_지역을_연결한다() {
        Region seoul = regionRepository.save(Region.create("11000", "서울특별시", "서울특별시"));

        PolicyWriteResult result =
                writer.upsertAll(
                        List.of(
                                new PolicyUpsertItem(
                                        policy("R2026-001", "새 정책", null), List.of(seoul), false)));

        assertThat(result).isEqualTo(new PolicyWriteResult(1, 0, 0));
        assertThat(policyRepository.findAll())
                .singleElement()
                .satisfies(saved -> assertThat(saved.getTitle()).isEqualTo("새 정책"));
        assertThat(policyRegionRepository.findAll())
                .singleElement()
                .satisfies(link -> assertThat(link.getRegion().getCode()).isEqualTo("11000"));
    }

    @Test
    void 기존_정책은_UPDATE하고_지역_링크를_교체하며_숨김_상태를_리셋한다() {
        Region seoul = regionRepository.save(Region.create("11000", "서울특별시", "서울특별시"));
        Region busan = regionRepository.save(Region.create("26000", "부산광역시", "부산광역시"));
        Policy existing = policy("R2026-001", "옛 제목", LocalDateTime.of(2026, 1, 1, 0, 0));
        existing.markMissing();
        existing.markMissing();
        existing.markMissing(); // HIDDEN — 재등장 시 리셋되는지 확인용
        existing = policyRepository.save(existing);
        policyRegionRepository.save(PolicyRegion.create(existing, seoul));

        PolicyWriteResult result =
                writer.upsertAll(
                        List.of(
                                new PolicyUpsertItem(
                                        policy(
                                                "R2026-001",
                                                "새 제목",
                                                LocalDateTime.of(2026, 7, 1, 0, 0)),
                                        List.of(busan),
                                        false)));

        assertThat(result).isEqualTo(new PolicyWriteResult(0, 1, 0));
        assertThat(policyRepository.findAll())
                .singleElement()
                .satisfies(
                        saved -> {
                            assertThat(saved.getTitle()).isEqualTo("새 제목");
                            assertThat(saved.getMissingCount()).isZero();
                            assertThat(saved.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
                        });
        assertThat(policyRegionRepository.findAll())
                .singleElement()
                .satisfies(link -> assertThat(link.getRegion().getCode()).isEqualTo("26000"));
    }

    @Test
    void 깨진_정책이_섞인_청크는_건별_재시도로_범인만_제외하고_저장한다() {
        // title은 NOT NULL — null이면 INSERT가 터지는 "범인"
        PolicyWriteResult result =
                writer.upsertAll(
                        List.of(
                                new PolicyUpsertItem(
                                        policy("R2026-001", "정상1", null), List.of(), false),
                                new PolicyUpsertItem(
                                        policy("R2026-002", null, null), List.of(), false),
                                new PolicyUpsertItem(
                                        policy("R2026-003", "정상2", null), List.of(), false)));

        assertThat(result).isEqualTo(new PolicyWriteResult(2, 0, 1));
        assertThat(policyRepository.findAll())
                .extracting(Policy::getPolicyNo)
                .containsExactlyInAnyOrder("R2026-001", "R2026-003");
    }

    @Test
    void 사라진_정책은_누락_횟수를_올리고_3회_도달_시_숨긴다() {
        Policy twiceMissed = policy("R2026-001", "2회 누락됨", null);
        twiceMissed.markMissing();
        twiceMissed.markMissing();
        policyRepository.save(twiceMissed);
        policyRepository.save(policy("R2026-002", "첫 누락", null));

        int marked = writer.hideMissing(List.of("R2026-001", "R2026-002", "R9999-999"));

        assertThat(marked).isEqualTo(2); // DB에 없는 번호는 무시
        Policy first = policyRepository.findByPolicyNoIn(List.of("R2026-001")).getFirst();
        Policy second = policyRepository.findByPolicyNoIn(List.of("R2026-002")).getFirst();
        assertThat(first.getMissingCount()).isEqualTo(3);
        assertThat(first.getVisibility()).isEqualTo(PolicyVisibility.HIDDEN);
        assertThat(second.getMissingCount()).isEqualTo(1);
        assertThat(second.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
    }
}
