package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bop.youthpick.global.error.CustomException;
import com.bop.youthpick.log.service.SearchLogService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.bop.youthpick.search.config.PolicySearchProperties;
import com.bop.youthpick.search.service.PolicySearchService;
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
    @Mock private PolicyRecentViewService policyRecentViewService;
    @Mock private SearchLogService searchLogService;
    @Mock private PolicySearchService policySearchService;

    private PolicyService policyService;

    @BeforeEach
    void setUp() {
        policyService =
                new PolicyService(
                        policyRepository,
                        policyRegionRepository,
                        policyRecentViewService,
                        searchLogService,
                        policySearchService,
                        // 이 테스트는 기존 MySQL 경로를 검증한다 — 검색 전환은 enabled=false 로 끈다.
                        new PolicySearchProperties(false, "policy"));
    }

    @Test
    void 노출_중인_정책이면_상세와_지역을_이름까지_반환한다() {
        Policy policy = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                        1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findWithRegionByPolicyIdIn(List.of(1L)))
                .thenReturn(List.of(newPolicyRegion(policy, "11680", "서울특별시", "강남구")));

        PolicyDetailResponse response = policyService.getDetail(1L, null);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("청년 월세 지원");
        assertThat(response.regions()).containsExactly(new RegionResponse("11680", "서울특별시", "강남구"));
        // #92: 자격 판정용 내부 코드도 상세 응답에 노출해 프론트 추천 하드필터를 복구할 수 있게 한다.
        assertThat(response.jobCodes()).isEqualTo("0013010");
        assertThat(response.schoolCodes()).isEqualTo("0043010");
        assertThat(response.maritalStatusCode()).isEqualTo("0055010");
        assertThat(response.majorCodes()).isEqualTo("0066010");
        assertThat(response.specializationCodes()).isEqualTo("0077010");
    }

    @Test
    void 없거나_삭제_숨김된_정책이면_POLICY_NOT_FOUND_예외를_던진다() {
        when(policyRepository.findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
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
        when(policyRepository.findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                        1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findWithRegionByPolicyIdIn(anyList())).thenReturn(List.of());

        policyService.getDetail(1L, 7L);

        verify(policyRecentViewService).record(7L, policy);
    }

    @Test
    void 기록_저장이_실패해도_상세_조회는_정상_응답한다() {
        Policy policy = newPolicy(1L, "청년 월세 지원");
        when(policyRepository.findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                        1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findWithRegionByPolicyIdIn(anyList())).thenReturn(List.of());
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
        when(policyRepository.findByIdAndVisibilityAndAdminHiddenFalseAndDeletedAtIsNull(
                        1L, PolicyVisibility.VISIBLE))
                .thenReturn(Optional.of(policy));
        when(policyRegionRepository.findWithRegionByPolicyIdIn(anyList())).thenReturn(List.of());

        policyService.getDetail(1L, null);

        verify(policyRecentViewService, never()).record(null, policy);
    }

    @Test
    void 목록은_VISIBLE_미삭제만_id_내림차순으로_조회하고_카드에_지역_시도명_목록을_담는다() {
        Policy single = newPolicy(1L, "한 시도 정책");
        ReflectionTestUtils.setField(single, "category", "주거");
        Policy noRegion = newPolicy(2L, "지역 없음 정책");
        Policy multi = newPolicy(3L, "두 시도 정책");
        Policy allSido = newPolicy(4L, "전 시도 정책");
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        pageableCaptor.capture()))
                .thenReturn(
                        new PageImpl<>(
                                List.of(single, noRegion, multi, allSido),
                                PageRequest.of(0, 20),
                                4));
        // 전국/외 N 같은 표시 문구는 더 이상 조립하지 않는다 — 시도명 목록(중복 제거·정렬)만 그대로 내려준다.
        when(policyRegionRepository.findWithRegionByPolicyIdIn(List.of(1L, 2L, 3L, 4L)))
                .thenReturn(
                        List.of(
                                newPolicyRegion(single, "11680", "서울특별시", "강남구"),
                                newPolicyRegion(multi, "11680", "서울특별시", "강남구"),
                                newPolicyRegion(multi, "26110", "부산광역시", "중구"),
                                newPolicyRegion(allSido, "11680", "서울특별시", "강남구"),
                                newPolicyRegion(allSido, "26110", "부산광역시", "중구"),
                                newPolicyRegion(allSido, "27110", "대구광역시", "중구")));

        Page<PolicyCardResponse> page =
                policyService.getCards(null, null, null, null, null, null, PageRequest.of(0, 20));

        // 정렬은 리포지토리 쿼리(order by)가 고정한다 — 서비스는 정렬 없는 Pageable을 넘긴다.
        assertThat(pageableCaptor.getValue().getSort()).isEqualTo(Sort.unsorted());
        PolicyCardResponse card = page.getContent().get(0);
        assertThat(card.id()).isEqualTo(1L);
        assertThat(card.title()).isEqualTo("한 시도 정책");
        assertThat(card.category()).isEqualTo("주거");
        // #92: 목록(카드) 응답도 상세와 동일하게 자격 판정용 내부 코드를 노출해 프론트 추천 하드필터를 복구할 수 있게 한다.
        assertThat(card.jobCodes()).isEqualTo("0013010");
        assertThat(card.schoolCodes()).isEqualTo("0043010");
        assertThat(card.maritalStatusCode()).isEqualTo("0055010");
        assertThat(card.majorCodes()).isEqualTo("0066010");
        assertThat(card.specializationCodes()).isEqualTo("0077010");
        assertThat(page.getContent())
                .extracting(PolicyCardResponse::provinces)
                .containsExactly(
                        List.of("서울특별시"),
                        List.of(),
                        List.of("부산광역시", "서울특별시"),
                        List.of("대구광역시", "부산광역시", "서울특별시"));
    }

    @Test
    void 검색어의_LIKE_특수문자를_이스케이프해_전달한다() {
        ArgumentCaptor<String> keywordCaptor = ArgumentCaptor.forClass(String.class);
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        keywordCaptor.capture(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        policyService.getCards(
                null, "50% 지원_special!", null, null, null, null, PageRequest.of(0, 20));

        assertThat(keywordCaptor.getValue()).isEqualTo("%50!% 지원!_special!!%");
    }

    @Test
    void region이_전국이면_지역_무관이라_시도명_필터를_걸지_않는다() {
        ArgumentCaptor<String> sidoNameCaptor = ArgumentCaptor.forClass(String.class);
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        isNull(),
                        sidoNameCaptor.capture(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        policyService.getCards(null, null, "전국", null, null, null, PageRequest.of(0, 20));

        assertThat(sidoNameCaptor.getValue()).isNull();
    }

    @Test
    void jobCode는_공백을_제거해_그대로_리포지토리에_전달한다() {
        ArgumentCaptor<String> jobCodeCaptor = ArgumentCaptor.forClass(String.class);
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        jobCodeCaptor.capture(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        policyService.getCards(null, null, null, null, null, "  0013001  ", PageRequest.of(0, 20));

        assertThat(jobCodeCaptor.getValue()).isEqualTo("0013001");
    }

    @Test
    void jobCode가_빈_문자열이면_필터를_걸지_않는다() {
        ArgumentCaptor<String> jobCodeCaptor = ArgumentCaptor.forClass(String.class);
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        jobCodeCaptor.capture(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        policyService.getCards(null, null, null, null, null, "   ", PageRequest.of(0, 20));

        assertThat(jobCodeCaptor.getValue()).isNull();
    }

    @Test
    void region이_시도명이면_그대로_시도명_필터로_전달한다() {
        ArgumentCaptor<String> sidoNameCaptor = ArgumentCaptor.forClass(String.class);
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        isNull(),
                        sidoNameCaptor.capture(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        policyService.getCards(null, null, "서울특별시", null, null, null, PageRequest.of(0, 20));

        assertThat(sidoNameCaptor.getValue()).isEqualTo("서울특별시");
    }

    @Test
    void age_구간을_그대로_ageMin_ageMax_파라미터로_전달한다() {
        ArgumentCaptor<Integer> ageMinCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> ageMaxCaptor = ArgumentCaptor.forClass(Integer.class);
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        isNull(),
                        isNull(),
                        ageMinCaptor.capture(),
                        ageMaxCaptor.capture(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        policyService.getCards(null, null, null, 19, 34, null, PageRequest.of(0, 20));

        assertThat(ageMinCaptor.getValue()).isEqualTo(19);
        assertThat(ageMaxCaptor.getValue()).isEqualTo(34);
    }

    private Policy newPolicy(Long id, String title) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "id", id);
        ReflectionTestUtils.setField(policy, "policyNo", "R2026" + id);
        ReflectionTestUtils.setField(policy, "title", title);
        ReflectionTestUtils.setField(policy, "jobCodes", "0013010");
        ReflectionTestUtils.setField(policy, "schoolCodes", "0043010");
        ReflectionTestUtils.setField(policy, "maritalStatusCode", "0055010");
        ReflectionTestUtils.setField(policy, "majorCodes", "0066010");
        ReflectionTestUtils.setField(policy, "specializationCodes", "0077010");
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
