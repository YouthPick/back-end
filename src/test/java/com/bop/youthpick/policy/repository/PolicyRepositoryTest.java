package com.bop.youthpick.policy.repository;

import static com.bop.youthpick.policy.entity.PolicyFixture.policy;
import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

// @DataJpaTest는 슬라이스 스캔 대상에서 일반 @Configuration을 제외하므로, BaseEntity의
// createdAt/updatedAt을 채우는 JpaAuditingConfig(@EnableJpaAuditing)를 직접 import한다.
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PolicyRepositoryTest {

    @Autowired private PolicyRepository policyRepository;

    @Test
    void 저장된_모든_정책의_비교용_스냅샷을_반환한다() {
        LocalDateTime modifiedAt = LocalDateTime.of(2026, 7, 1, 10, 30);
        policyRepository.save(policy("R2026-001", "테스트 정책", modifiedAt));
        // lastMdfcnDt가 비어 오는 정책도 실존한다(원천 데이터 품질) — 스냅샷에서 null 유지 확인
        policyRepository.save(policy("R2026-002", "테스트 정책", null));

        List<PolicySyncSnapshot> snapshots = policyRepository.findSyncSnapshots();

        assertThat(snapshots)
                .extracting(PolicySyncSnapshot::policyNo)
                .containsExactlyInAnyOrder("R2026-001", "R2026-002");
        PolicySyncSnapshot first =
                snapshots.stream()
                        .filter(s -> s.policyNo().equals("R2026-001"))
                        .findFirst()
                        .orElseThrow();
        assertThat(first.lastModifiedAt()).isEqualTo(modifiedAt);
        assertThat(first.visibility()).isEqualTo(PolicyVisibility.VISIBLE);
        PolicySyncSnapshot second =
                snapshots.stream()
                        .filter(s -> s.policyNo().equals("R2026-002"))
                        .findFirst()
                        .orElseThrow();
        assertThat(second.lastModifiedAt()).isNull();
    }

    @Test
    void 정책이_없으면_빈_스냅샷을_반환한다() {
        assertThat(policyRepository.findSyncSnapshots()).isEmpty();
    }

    @Test
    void 숨김_처리된_정책도_스냅샷에_포함된다() {
        var hidden = policy("R2026-003", "숨김 정책", null);
        hidden.markMissing();
        hidden.markMissing();
        hidden.markMissing(); // 3회 누락 → HIDDEN
        policyRepository.save(hidden);

        assertThat(policyRepository.findSyncSnapshots())
                .singleElement()
                .satisfies(s -> assertThat(s.visibility()).isEqualTo(PolicyVisibility.HIDDEN));
    }
}
