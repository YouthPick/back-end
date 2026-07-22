package com.bop.youthpick.policy.repository;

import static com.bop.youthpick.policy.entity.PolicyFixture.policy;
import static org.assertj.core.api.Assertions.assertThat;

import com.bop.youthpick.global.config.JpaAuditingConfig;
import com.bop.youthpick.policy.dto.PolicySyncSnapshot;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

// @DataJpaTest는 슬라이스 스캔 대상에서 일반 @Configuration을 제외하므로, BaseEntity의
// createdAt/updatedAt을 채우는 JpaAuditingConfig(@EnableJpaAuditing)를 직접 import한다.
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PolicyRepositoryTest {

    @Autowired private PolicyRepository policyRepository;
    @Autowired private RegionRepository regionRepository;
    @Autowired private PolicyRegionRepository policyRegionRepository;

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

    @Test
    void 신청마감일이_없어도_사업기간이_이미_지났으면_카드_목록에서_제외한다() {
        LocalDate today = LocalDate.of(2026, 7, 21);
        // 신청기간 정보가 없는(0057003류) 일회성 모집 — 사업기간은 이미 끝났다: 제외돼야 한다.
        policyRepository.save(
                policyForCards("P001", null, LocalDate.of(2026, 3, 31), today.minusDays(1)));
        // 신청기간 정보가 없고 사업기간도 없는 진짜 상시(0057002류): 포함돼야 한다.
        policyRepository.save(policyForCards("P002", null, null, null));
        // 사업기간이 아직 안 끝난 경우: 포함돼야 한다.
        policyRepository.save(
                policyForCards("P003", null, LocalDate.of(2026, 1, 1), today.plusDays(1)));
        // 신청마감일 자체가 지난 경우: 기존 조건으로도 이미 제외된다.
        policyRepository.save(policyForCards("P004", today.minusDays(1), null, null));

        List<Policy> result =
                policyRepository
                        .findCards(
                                PolicyVisibility.VISIBLE,
                                today,
                                null,
                                null,
                                null,
                                null,
                                null,
                                null,
                                PageRequest.of(0, 20))
                        .getContent();

        assertThat(result)
                .extracting(Policy::getPolicyNo)
                .containsExactlyInAnyOrder("P002", "P003");
    }

    @Test
    void jobCode_필터는_해당_코드와_제한없음_정책을_함께_통과시키고_제한없음을_뒤로_민다() {
        // 재직자 전용 / 미취업자 전용 / 제한없음 / 다중값(재직+미취업)
        savePolicyWithJobCodes("J-EMPLOYED", "0013001");
        savePolicyWithJobCodes("J-UNEMPLOYED", "0013003");
        savePolicyWithJobCodes("J-ANY", Policy.JOB_CODE_UNRESTRICTED);
        savePolicyWithJobCodes("J-MULTI", "0013001,0013003");

        Page<Policy> page = findCards("0013001");

        // 미취업자 전용만 빠지고, 제한없음은 남되 맨 뒤로 간다.
        assertThat(page.getContent())
                .extracting(Policy::getPolicyNo)
                .containsExactly("J-MULTI", "J-EMPLOYED", "J-ANY");
        assertThat(page.getTotalElements()).isEqualTo(3);
    }

    @Test
    void jobCode가_없으면_취업상태로_거르지도_정렬하지도_않는다() {
        savePolicyWithJobCodes("J-ANY", Policy.JOB_CODE_UNRESTRICTED);
        savePolicyWithJobCodes("J-EMPLOYED", "0013001");

        Page<Policy> page = findCards(null);

        // 최신순(id desc)만 적용 — 제한없음이 뒤로 밀리지 않는다.
        assertThat(page.getContent())
                .extracting(Policy::getPolicyNo)
                .containsExactly("J-EMPLOYED", "J-ANY");
    }

    @Test
    void jobCode_매칭은_구분자로_감싸_부분일치_사고를_막는다() {
        // '0013001'로 검색할 때 '10013001'이나 '00130010'이 걸리면 안 된다.
        savePolicyWithJobCodes("J-PREFIXED", "10013001");
        savePolicyWithJobCodes("J-SUFFIXED", "00130010");
        savePolicyWithJobCodes("J-EXACT", "0013001");

        Page<Policy> page = findCards("0013001");

        assertThat(page.getContent()).extracting(Policy::getPolicyNo).containsExactly("J-EXACT");
    }

    @Test
    void 시도를_고르면_전국_정책을_뒤로_밀어_그_지역_전용_정책이_먼저_나온다() {
        Region sejong = regionRepository.save(Region.create("29000", "세종특별자치시", "세종특별자치시"));
        Region seoul = regionRepository.save(Region.create("11000", "서울특별시", "서울특별시"));
        // 전국 정책도 세종에 링크가 있어 시도 필터에 함께 걸린다 — 그래서 정렬로 밀어내야 한다.
        Policy nationwide = saveLinked("P-ALL", true, sejong, seoul);
        Policy sejongOnly = saveLinked("P-SEJONG", false, sejong);

        Page<Policy> page =
                policyRepository.findCards(
                        PolicyVisibility.VISIBLE,
                        LocalDate.now(),
                        null,
                        null,
                        "세종특별자치시",
                        null,
                        null,
                        null,
                        PageRequest.of(0, 20));

        assertThat(page.getContent())
                .extracting(Policy::getPolicyNo)
                .containsExactly(sejongOnly.getPolicyNo(), nationwide.getPolicyNo());
    }

    @Test
    void 지역을_고르지_않으면_전국_정책을_뒤로_밀지_않는다() {
        Region sejong = regionRepository.save(Region.create("29000", "세종특별자치시", "세종특별자치시"));
        Policy nationwide = saveLinked("P-ALL", true, sejong);
        Policy normal = saveLinked("P-NORMAL", false, sejong);

        Page<Policy> page = findCards(null);

        // 최신순(id desc)만 — 나중에 저장된 P-NORMAL이 앞선다.
        assertThat(page.getContent())
                .extracting(Policy::getPolicyNo)
                .containsExactly(normal.getPolicyNo(), nationwide.getPolicyNo());
    }

    private Policy saveLinked(String policyNo, boolean nationwide, Region... regions) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        ReflectionTestUtils.setField(policy, "nationwide", nationwide);
        Policy saved = policyRepository.save(policy);
        for (Region region : regions) {
            policyRegionRepository.save(PolicyRegion.create(saved, region));
        }
        return saved;
    }

    private Page<Policy> findCards(String jobCode) {
        return policyRepository.findCards(
                PolicyVisibility.VISIBLE,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                jobCode,
                PageRequest.of(0, 20));
    }

    private void savePolicyWithJobCodes(String policyNo, String jobCodes) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "policyNo", policyNo);
        ReflectionTestUtils.setField(policy, "title", policyNo + " title");
        ReflectionTestUtils.setField(policy, "visibility", PolicyVisibility.VISIBLE);
        ReflectionTestUtils.setField(policy, "jobCodes", jobCodes);
        policyRepository.save(policy);
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

    private Policy policyForCards(
            String policyNo,
            LocalDate applicationEndDate,
            LocalDate businessPeriodBegin,
            LocalDate businessPeriodEnd) {
        Policy policy = newPolicy(policyNo, PolicyVisibility.VISIBLE, null);
        ReflectionTestUtils.setField(policy, "applicationEndDate", applicationEndDate);
        ReflectionTestUtils.setField(policy, "businessPeriodBegin", businessPeriodBegin);
        ReflectionTestUtils.setField(policy, "businessPeriodEnd", businessPeriodEnd);
        return policy;
    }
}
