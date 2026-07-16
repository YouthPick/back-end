package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyRegionRepository policyRegionRepository;
    @Mock private PolicyRecentViewService policyRecentViewService;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService =
                new PolicyService(
                        policyRepository, policyRegionRepository, policyRecentViewService);
    }

    @Test
    void 노출_중인_정책이면_상세와_지역코드를_반환한다() {
        Policy policy = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findByIdAndVisibilityAndDeletedAtIsNull(1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findByPolicyIdIn(List.of(1L)))
                .thenReturn(List.of(newPolicyRegion(policy, "11110")));

        PolicyDetailResponse response = policyService.getDetail(1L, null);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("청년 월세 지원");
        assertThat(response.regionCodes()).containsExactly("11110");
    }

    @Test
    void 없거나_삭제_숨김된_정책이면_POLICY_NOT_FOUND_예외를_던진다() {
        when(policyRepository.findByIdAndVisibilityAndDeletedAtIsNull(
                        99L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> policyService.getDetail(99L, 1L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.POLICY_NOT_FOUND);
        verify(policyRecentViewService, never()).record(1L, null);
    }

    @Test
    void 로그인_사용자의_조회는_최근_본_정책으로_기록한다() {
        Policy policy = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findByIdAndVisibilityAndDeletedAtIsNull(1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findByPolicyIdIn(anyList())).thenReturn(List.of());

        policyService.getDetail(1L, 7L);

        verify(policyRecentViewService).record(7L, policy);
    }

    @Test
    void 기록_저장이_실패해도_상세_조회는_정상_응답한다() {
        Policy policy = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findByIdAndVisibilityAndDeletedAtIsNull(1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findByPolicyIdIn(anyList())).thenReturn(List.of());
        doThrow(new DataAccessResourceFailureException("DB 연결 실패"))
                .when(policyRecentViewService)
                .record(7L, policy);

        PolicyDetailResponse response = policyService.getDetail(1L, 7L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("청년 월세 지원");
    }

    @Test
    void 비회원_조회는_기록하지_않는다() {
        Policy policy = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findByIdAndVisibilityAndDeletedAtIsNull(1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findByPolicyIdIn(anyList())).thenReturn(List.of());

        policyService.getDetail(1L, null);

        verify(policyRecentViewService, never()).record(null, policy);
    }

    private Policy newPolicy(Long id, String title) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "id", id);
        ReflectionTestUtils.setField(policy, "policyNo", "R2026" + id);
        ReflectionTestUtils.setField(policy, "title", title);
        return policy;
    }

    private PolicyRegion newPolicyRegion(Policy policy, String regionCode) {
        Region region = BeanUtils.instantiateClass(Region.class);
        ReflectionTestUtils.setField(region, "code", regionCode);
        return PolicyRegion.create(policy, region);
    }
}
