package com.bop.youthpick.policy.repository;

import static com.bop.youthpick.policy.entity.PolicyFixture.policy;
import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    void visibility별_soft_delete되지_않은_정책만_카운트한다() {
        policyRepository.save(newPolicy("P001", PolicyVisibility.VISIBLE, null));
        policyRepository.save(newPolicy("P002", PolicyVisibility.VISIBLE, null));
        policyRepository.save(
                newPolicy("P003", PolicyVisibility.VISIBLE, LocalDateTime.now())); // soft delete
        policyRepository.save(newPolicy("P004", PolicyVisibility.HIDDEN, null));

        assertThat(policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.VISIBLE))
                .isEqualTo(2);
        assertThat(policyRepository.countByVisibilityAndDeletedAtIsNull(PolicyVisibility.HIDDEN))
                .isEqualTo(1);
    }

    // RecommendedPolicyService는 점수 계산을 위해 후보를 페이지 없이(Pageable.unpaged()) 전량 조회한다.
    // 목록 조회(getCards)는 항상 PageRequest를 넘기므로 이 경로는 여기서만 실제로 실행된다.
    @Test
    void 카드_조회는_unpaged와_나이_조건_없이도_노출_중인_정책만_반환한다() {
        policyRepository.save(newPolicy("P001", PolicyVisibility.VISIBLE, null));
        policyRepository.save(newPolicy("P002", PolicyVisibility.VISIBLE, LocalDateTime.now()));
        policyRepository.save(newPolicy("P003", PolicyVisibility.HIDDEN, null));

        Page<Policy> page =
                policyRepository.findCards(
                        PolicyVisibility.VISIBLE,
                        LocalDate.now(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        Pageable.unpaged());

        assertThat(page.getContent()).extracting(Policy::getPolicyNo).containsExactly("P001");
    }

    private Policy newPolicy(
            String policyNo, PolicyVisibility visibility, LocalDateTime deletedAt) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "visibility", visibility);
        ReflectionTestUtils.setField(policy, "deletedAt", deletedAt);
        return policy;
    }
}
