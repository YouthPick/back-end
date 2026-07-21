package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyComparisonItemResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyComparisonServiceTest {

    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyRegionRepository policyRegionRepository;

    private PolicyComparisonService policyComparisonService;

    @BeforeEach
    void setUp() {
        policyComparisonService =
                new PolicyComparisonService(policyRepository, policyRegionRepository);
    }

    @Test
    void 요청한_순서_그대로_비교_대상_정책을_반환한다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        Policy second = newPolicy(2L, "청년 취업 장려금");
        // repository가 요청과 다른 순서로 반환해도 응답은 요청 순서(2, 1)를 따라야 한다.
        when(policyRepository.findAllById(List.of(2L, 1L))).thenReturn(List.of(first, second));

        List<PolicyComparisonItemResponse> policies =
                policyComparisonService.compare(List.of(2L, 1L));

        assertThat(policies).extracting("policyId").containsExactly(2L, 1L);
        assertThat(policies).extracting("title").containsExactly("청년 취업 장려금", "청년 월세 지원");
    }

    @Test
    void 정책별_지역을_묶어서_담고_지역이_없는_정책은_빈_목록으로_내려준다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        Policy second = newPolicy(2L, "청년 취업 장려금");
        when(policyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));
        when(policyRegionRepository.findWithRegionByPolicyIdIn(List.of(1L, 2L)))
                .thenReturn(
                        List.of(
                                newPolicyRegion(first, "11680", "서울특별시", "강남구"),
                                newPolicyRegion(first, "26110", "부산광역시", "중구")));

        List<PolicyComparisonItemResponse> policies =
                policyComparisonService.compare(List.of(1L, 2L));

        assertThat(policies.get(0).regions())
                .containsExactly(
                        new RegionResponse("11680", "서울특별시", "강남구"),
                        new RegionResponse("26110", "부산광역시", "중구"));
        assertThat(policies.get(1).regions()).isEmpty();
    }

    @Test
    void policyIds에_중복이_있으면_INVALID_COMPARISON_REQUEST_예외를_던진다() {
        assertThatThrownBy(() -> policyComparisonService.compare(List.of(1L, 1L)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue(
                        "errorCode", PolicyErrorCode.INVALID_COMPARISON_REQUEST);
        verify(policyRepository, never()).findAllById(List.of(1L, 1L));
    }

    @Test
    void 존재하지_않는_정책이_섞여있으면_POLICY_NOT_FOUND_예외를_던진다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first));

        assertThatThrownBy(() -> policyComparisonService.compare(List.of(1L, 2L)))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    void 정책_3개도_정상_비교된다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        Policy second = newPolicy(2L, "청년 취업 장려금");
        Policy third = newPolicy(3L, "청년 도약계좌");
        when(policyRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(first, second, third));

        List<PolicyComparisonItemResponse> policies =
                policyComparisonService.compare(List.of(1L, 2L, 3L));

        assertThat(policies).extracting("policyId").containsExactly(1L, 2L, 3L);
    }

    private Policy newPolicy(Long id, String title) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "id", id);
        ReflectionTestUtils.setField(policy, "policyNo", "R2026" + id);
        ReflectionTestUtils.setField(policy, "title", title);
        return policy;
    }

    private PolicyRegion newPolicyRegion(
            Policy policy, String regionCode, String sidoName, String name) {
        Region region = BeanUtils.instantiateClass(Region.class);
        ReflectionTestUtils.setField(region, "code", regionCode);
        ReflectionTestUtils.setField(region, "sidoName", sidoName);
        ReflectionTestUtils.setField(region, "name", name);
        return PolicyRegion.create(policy, region);
    }
}
