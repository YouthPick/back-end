package com.bop.youthpick.admin.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.repository.PolicyRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class AdminPolicySpecificationsTest {

    @Autowired private PolicyRepository policyRepository;

    @BeforeEach
    void setUp() {
        policyRepository.save(
                newPolicy(
                        "P001",
                        "일자리",
                        PolicyVisibility.VISIBLE,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2026, 3, 31)));
        policyRepository.save(
                newPolicy(
                        "P002",
                        "주거",
                        PolicyVisibility.HIDDEN,
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2026, 8, 31)));
        policyRepository.save(newPolicy("P003", "일자리", PolicyVisibility.VISIBLE, null, null));
    }

    // 배치 수집(온통청년 API) 경로가 아직 없어 Policy 인스턴스를 만들 공개 팩토리가 없다.
    // 테스트 전용으로 ReflectionTestUtils를 사용해 필드를 채운다.
    private Policy newPolicy(
            String policyNo,
            String category,
            PolicyVisibility visibility,
            LocalDate applicationStartDate,
            LocalDate applicationEndDate) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "category", category);
        ReflectionTestUtils.setField(policy, "visibility", visibility);
        ReflectionTestUtils.setField(policy, "applicationStartDate", applicationStartDate);
        ReflectionTestUtils.setField(policy, "applicationEndDate", applicationEndDate);
        return policy;
    }

    @Test
    void category로_필터링한다() {
        List<Policy> result =
                policyRepository.findAll(AdminPolicySpecifications.filter("일자리", null, null, null));

        assertThat(result)
                .extracting(Policy::getPolicyNo)
                .containsExactlyInAnyOrder("P001", "P003");
    }

    @Test
    void visibilityStatus로_필터링한다() {
        List<Policy> result =
                policyRepository.findAll(
                        AdminPolicySpecifications.filter(null, "HIDDEN", null, null));

        assertThat(result).extracting(Policy::getPolicyNo).containsExactly("P002");
    }

    @Test
    void 신청기간이_겹치는_정책만_반환한다() {
        List<Policy> result =
                policyRepository.findAll(
                        AdminPolicySpecifications.filter(
                                null, null, LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28)));

        assertThat(result).extracting(Policy::getPolicyNo).containsExactly("P001");
    }

    @Test
    void 신청기간이_없는_정책은_기간_필터에서_제외된다() {
        List<Policy> result =
                policyRepository.findAll(
                        AdminPolicySpecifications.filter(
                                null, null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)));

        assertThat(result)
                .extracting(Policy::getPolicyNo)
                .containsExactlyInAnyOrder("P001", "P002");
    }

    @Test
    void 필터가_없으면_전체를_반환한다() {
        List<Policy> result =
                policyRepository.findAll(AdminPolicySpecifications.filter(null, null, null, null));

        assertThat(result).hasSize(3);
    }
}
