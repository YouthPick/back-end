package com.bop.youthpick.policy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@Import(JpaAuditingConfig.class)
class PolicyRegionRepositoryTest {

    @Autowired private PolicyRepository policyRepository;

    @Autowired private RegionRepository regionRepository;

    @Autowired private PolicyRegionRepository policyRegionRepository;

    @Test
    void 정책ID로_지역_매핑을_모두_삭제한다() {
        Region region = regionRepository.save(newRegion("11110", "서울특별시", "강남구"));
        Policy policy = policyRepository.save(newPolicy("P001"));
        policyRegionRepository.save(PolicyRegion.create(policy, region));

        policyRegionRepository.deleteByPolicyId(policy.getId());

        assertThat(policyRegionRepository.findByPolicyIdIn(List.of(policy.getId()))).isEmpty();
    }

    @Test
    void 정책ID_목록으로_지역코드를_한번에_조회한다() {
        Region seoul = regionRepository.save(newRegion("11110", "서울특별시", "강남구"));
        Region busan = regionRepository.save(newRegion("26110", "부산광역시", "중구"));
        Policy policy1 = policyRepository.save(newPolicy("P001"));
        Policy policy2 = policyRepository.save(newPolicy("P002"));
        policyRegionRepository.save(PolicyRegion.create(policy1, seoul));
        policyRegionRepository.save(PolicyRegion.create(policy2, busan));

        List<PolicyRegion> result =
                policyRegionRepository.findByPolicyIdIn(List.of(policy1.getId(), policy2.getId()));

        assertThat(result).hasSize(2);
    }

    private Region newRegion(String code, String sidoName, String name) {
        Region region = BeanUtils.instantiateClass(Region.class);
        ReflectionTestUtils.setField(region, "code", code);
        ReflectionTestUtils.setField(region, "sidoName", sidoName);
        ReflectionTestUtils.setField(region, "name", name);
        return region;
    }

    private Policy newPolicy(String policyNo) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        return policy;
    }
}
