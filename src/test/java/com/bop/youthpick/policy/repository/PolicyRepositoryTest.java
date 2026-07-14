package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.Policy;
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
        policyRepository.save(policy("R2026-001", modifiedAt));
        // lastMdfcnDt가 비어 오는 정책도 실존한다(원천 데이터 품질) — 스냅샷에서 null 유지 확인
        policyRepository.save(policy("R2026-002", null));

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

    // HIDDEN 정책 포함 검증은 visibility 상태 변경 메서드가 생기는 3-2에서 추가한다.

    /** 스냅샷 비교에 쓰이는 필드만 채운 최소 정책. 파라미터 순서 = Policy 필드 선언 순서. */
    private Policy policy(String policyNo, LocalDateTime lastModifiedAt) {
        return Policy.create(
                policyNo,
                "테스트 정책",
                null,
                null,
                null,
                null,
                null,
                null, // description~organizationName
                null,
                null, // minAge, maxAge
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null, // jobCodes~participationRestriction
                null,
                null, // applicationPeriodType, applicationPeriodRaw
                null,
                null,
                null,
                null, // 날짜 4종
                null,
                null, // businessPeriodEtc, supportScaleCount
                false, // firstComeFirstServed
                null,
                null,
                null,
                null,
                null,
                null,
                null, // applicationUrl~ageLimitFlag
                null,
                null, // incomeMinAmount, supportScaleLimit
                null,
                null,
                null, // operatingInstitutionName~etcMatters
                0, // viewCount
                null, // firstRegisteredAt
                lastModifiedAt,
                null); // rawPayload
    }
}
