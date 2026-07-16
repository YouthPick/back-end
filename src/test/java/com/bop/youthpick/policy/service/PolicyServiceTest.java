package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyCardResponse;
import com.bop.youthpick.policy.dto.PolicyDetailResponse;
import com.bop.youthpick.policy.dto.RegionResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.policy.repository.RegionRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PolicyServiceTest {

    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyRegionRepository policyRegionRepository;
    @Mock private RegionRepository regionRepository;
    @Mock private PolicyRecentViewService policyRecentViewService;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService =
                new PolicyService(
                        policyRepository,
                        policyRegionRepository,
                        regionRepository,
                        policyRecentViewService);
    }

    @Test
    void 노출_중인_정책이면_상세와_지역을_이름까지_반환한다() {
        Policy policy = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findByIdAndVisibilityAndDeletedAtIsNull(1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findByPolicyIdIn(List.of(1L)))
                .thenReturn(List.of(newPolicyRegion(policy, "11680", "서울특별시", "강남구")));

        PolicyDetailResponse response = policyService.getDetail(1L, null);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("청년 월세 지원");
        assertThat(response.regions()).containsExactly(new RegionResponse("11680", "서울특별시", "강남구"));
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

    @Test
    void 목록은_VISIBLE_미삭제만_id_내림차순으로_조회하고_카드와_지역라벨을_조립한다() {
        Policy single = newPolicy(1L, "한 시도 정책");
        ReflectionTestUtils.setField(single, "category", "주거");
        Policy noRegion = newPolicy(2L, "지역 없음 정책");
        Policy multi = newPolicy(3L, "두 시도 정책");
        Policy nationwide = newPolicy(4L, "전국 정책");
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(policyRepository.findByVisibilityAndDeletedAtIsNull(
                        eq(PolicyVisibility.VISIBLE), pageableCaptor.capture()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(single, noRegion, multi, nationwide),
                                PageRequest.of(0, 20),
                                4));
        // 시도 전체 3개 기준 — single 1개, multi 2개(가나다 첫 시도 '부산광역시'), nationwide 3개(전국)
        when(policyRegionRepository.findWithRegionByPolicyIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(
                        List.of(
                                newPolicyRegion(single, "11680", "서울특별시", "강남구"),
                                newPolicyRegion(multi, "11680", "서울특별시", "강남구"),
                                newPolicyRegion(multi, "26110", "부산광역시", "중구"),
                                newPolicyRegion(nationwide, "11680", "서울특별시", "강남구"),
                                newPolicyRegion(nationwide, "26110", "부산광역시", "중구"),
                                newPolicyRegion(nationwide, "27110", "대구광역시", "중구")));
        when(regionRepository.countDistinctSidoNames()).thenReturn(3L);

        Page<PolicyCardResponse> page = policyService.getCards(PageRequest.of(0, 20));

        assertThat(pageableCaptor.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.DESC, "id"));
        PolicyCardResponse card = page.getContent().get(0);
        assertThat(card.id()).isEqualTo(1L);
        assertThat(card.title()).isEqualTo("한 시도 정책");
        assertThat(card.category()).isEqualTo("주거");
        assertThat(page.getContent())
                .extracting(PolicyCardResponse::regionLabel)
                .containsExactly("서울특별시", null, "부산광역시 외 1", "전국");
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
