package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.policy.dto.PolicyComparisonCreateRequest;
import com.bop.youthpick.policy.dto.PolicyComparisonResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.exception.PolicyErrorCode;
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

    private PolicyComparisonService policyComparisonService;

    @BeforeEach
    void setUp() {
        policyComparisonService = new PolicyComparisonService(policyRepository);
    }

    @Test
    void 생성_요청_순서와_무관하게_policyId_오름차순으로_정렬된_comparisonId를_발급한다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        Policy second = newPolicy(2L, "청년 취업 장려금");
        when(policyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        PolicyComparisonResponse response =
                policyComparisonService.create(new PolicyComparisonCreateRequest(List.of(2L, 1L)));

        assertThat(response.comparisonId()).isEqualTo("1-2");
        assertThat(response.policies()).extracting("policyId").containsExactly(1L, 2L);
    }

    @Test
    void 생성_요청에_중복된_policyId가_있으면_제거한_뒤_비교한다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        Policy second = newPolicy(2L, "청년 취업 장려금");
        when(policyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        PolicyComparisonResponse response =
                policyComparisonService.create(
                        new PolicyComparisonCreateRequest(List.of(1L, 1L, 2L)));

        assertThat(response.comparisonId()).isEqualTo("1-2");
    }

    @Test
    void 생성_존재하지_않는_정책이_포함되면_POLICY_NOT_FOUND_예외를_던진다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first));

        assertThatThrownBy(
                        () ->
                                policyComparisonService.create(
                                        new PolicyComparisonCreateRequest(List.of(1L, 2L))))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.POLICY_NOT_FOUND);
    }

    @Test
    void 조회_comparisonId를_policyId_목록으로_분해해_그_시점의_정책_정보를_반환한다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        Policy second = newPolicy(2L, "청년 취업 장려금");
        when(policyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(first, second));

        PolicyComparisonResponse response = policyComparisonService.find("1-2");

        assertThat(response.comparisonId()).isEqualTo("1-2");
        assertThat(response.policies()).extracting("policyId").containsExactly(1L, 2L);
        assertThat(response.policies())
                .extracting("title")
                .containsExactly("청년 월세 지원", "청년 취업 장려금");
    }

    @Test
    void 조회_숫자가_아닌_값이_섞여있으면_COMPARISON_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> policyComparisonService.find("1-abc"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.COMPARISON_NOT_FOUND);
    }

    @Test
    void 조회_policyId가_1개뿐이면_COMPARISON_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> policyComparisonService.find("1"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.COMPARISON_NOT_FOUND);
    }

    @Test
    void 조회_policyId가_4개_이상이면_COMPARISON_NOT_FOUND_예외를_던진다() {
        // create는 최대 3개까지만 허용하므로, 생성할 수 없는 comparisonId는 조회도 거부해야 한다.
        assertThatThrownBy(() -> policyComparisonService.find("1-2-3-4"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.COMPARISON_NOT_FOUND);
    }

    @Test
    void 조회_policyId가_3개면_정상_조회된다() {
        Policy first = newPolicy(1L, "청년 월세 지원");
        Policy second = newPolicy(2L, "청년 취업 장려금");
        Policy third = newPolicy(3L, "청년 도약계좌");
        when(policyRepository.findAllById(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(first, second, third));

        PolicyComparisonResponse response = policyComparisonService.find("1-2-3");

        assertThat(response.comparisonId()).isEqualTo("1-2-3");
        assertThat(response.policies()).extracting("policyId").containsExactly(1L, 2L, 3L);
    }

    @Test
    void 조회_참조하는_정책이_더_이상_없으면_POLICY_NOT_FOUND_예외를_던진다() {
        when(policyRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of());

        assertThatThrownBy(() -> policyComparisonService.find("1-2"))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", PolicyErrorCode.POLICY_NOT_FOUND);
    }

    private Policy newPolicy(Long id, String title) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "id", id);
        ReflectionTestUtils.setField(policy, "policyNo", "R2026" + id);
        ReflectionTestUtils.setField(policy, "title", title);
        return policy;
    }
}
