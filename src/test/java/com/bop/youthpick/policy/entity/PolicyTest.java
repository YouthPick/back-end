package com.bop.youthpick.policy.entity;

import static com.bop.youthpick.policy.entity.PolicyFixture.policy;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PolicyTest {

    @Test
    void 수집에서_2회_연속_사라져도_아직_보인다() {
        Policy policy = policy("R2026-001", "제목", null);

        policy.markMissing();
        policy.markMissing();

        assertThat(policy.getMissingCount()).isEqualTo(2);
        assertThat(policy.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
    }

    @Test
    void 수집에서_3회_연속_사라지면_숨김_처리된다() {
        Policy policy = policy("R2026-001", "제목", null);

        policy.markMissing();
        policy.markMissing();
        policy.markMissing();

        assertThat(policy.getMissingCount()).isEqualTo(3);
        assertThat(policy.getVisibility()).isEqualTo(PolicyVisibility.HIDDEN);
    }

    @Test
    void updateFrom은_내용을_갱신하고_누락_상태를_리셋한다() {
        Policy policy = policy("R2026-001", "옛 제목", LocalDateTime.of(2026, 1, 1, 0, 0));
        policy.markMissing();
        policy.markMissing();
        policy.markMissing(); // HIDDEN 상태에서 재등장하는 시나리오

        LocalDateTime freshModifiedAt = LocalDateTime.of(2026, 7, 1, 12, 0);
        policy.updateFrom(policy("R2026-001", "새 제목", freshModifiedAt));

        assertThat(policy.getTitle()).isEqualTo("새 제목");
        assertThat(policy.getLastModifiedAt()).isEqualTo(freshModifiedAt);
        assertThat(policy.getMissingCount()).isZero();
        assertThat(policy.getVisibility()).isEqualTo(PolicyVisibility.VISIBLE);
    }
}
