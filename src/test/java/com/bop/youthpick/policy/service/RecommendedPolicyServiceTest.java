package com.bop.youthpick.policy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.bop.youthpick.policy.dto.RecommendedPolicyResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyRegion;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.entity.Region;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.User;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RecommendedPolicyServiceTest {

    private static final String SEOUL = "서울특별시";
    private static final String BUSAN = "부산광역시";

    /** 프로필: 만 30세 · 서울 · 미취업(0013003) · 대학졸업(0049007) · 관심분야 일자리 · 키워드 청년 */
    private static final String USER_JOB_CODE = "0013003";

    private static final String USER_SCHOOL_CODE = "0049007";

    @Mock private PolicyRepository policyRepository;
    @Mock private PolicyRegionRepository policyRegionRepository;
    @Mock private UserProfileRepository userProfileRepository;

    private RecommendedPolicyService recommendedPolicyService;

    @BeforeEach
    void setUp() {
        recommendedPolicyService =
                new RecommendedPolicyService(
                        policyRepository, policyRegionRepository, userProfileRepository);
    }

    @Test
    void 프로필이_없으면_PROFILE_NOT_FOUND_예외를_던진다() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> recommendedPolicyService.getRecommendations(1L, null, null, null))
                .isInstanceOf(UserException.class)
                .hasFieldOrPropertyWithValue("errorCode", UserError.PROFILE_NOT_FOUND);
    }

    @Test
    void 모든_축이_일치하면_100점을_받는다() {
        Policy policy = newPolicy(1L, "청년 일자리 지원");
        setFields(policy, 20, 39, USER_JOB_CODE, USER_SCHOOL_CODE, "일자리", "청년,지원");
        // 프로필: 미혼(0055002) · 인문계열(0011001) · 여성(0014002) · 연소득 3000
        ReflectionTestUtils.setField(policy, "maritalStatusCode", "0055002");
        ReflectionTestUtils.setField(policy, "majorCodes", "0011001");
        ReflectionTestUtils.setField(policy, "specializationCodes", "0014002");
        ReflectionTestUtils.setField(policy, "incomeConditionCode", "0043002");
        ReflectionTestUtils.setField(policy, "incomeMaxAmount", 5000);
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        List<RecommendedPolicyResponse> result = recommend();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualTo(100);
        assertThat(result.get(0).matchedAxes())
                .containsExactlyInAnyOrder(
                        "분야", "지역", "특화조건", "취업", "키워드", "학력", "전공", "결혼여부", "나이", "소득");
    }

    @Test
    void 자격_나이를_벗어난_정책은_제외한다() {
        Policy policy = newPolicy(1L, "만 20~24세 정책");
        setFields(policy, 20, 24, USER_JOB_CODE, USER_SCHOOL_CODE, "일자리", "청년");
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 다른_시도_전용_정책은_제외한다() {
        Policy policy = newPolicy(1L, "부산 전용 정책");
        setFields(policy, null, null, USER_JOB_CODE, USER_SCHOOL_CODE, "일자리", "청년");
        givenCandidates(List.of(policy), Map.of(1L, List.of(BUSAN)));

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 사용자_취업상태를_대상으로_하지_않는_정책은_제외한다() {
        Policy policy = newPolicy(1L, "재직자 전용 정책");
        // 0013001 = 재직자. 미취업(0013003) 사용자는 자격 미달.
        setFields(policy, null, null, "0013001", USER_SCHOOL_CODE, "일자리", "청년");
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 제한없음_정책은_제외하지_않지만_취업_학력_가점도_주지_않는다() {
        Policy policy = newPolicy(1L, "누구나 신청 가능 정책");
        // 0013010 = 취업무관, 0049010 = 학력무관, 나이·관심사도 불일치 → 지역(18)만 남는다.
        setFields(policy, null, null, "0013010", "0049010", "주거", "전세");
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        List<RecommendedPolicyResponse> result = recommend();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isEqualTo(18);
        assertThat(result.get(0).matchedAxes()).containsExactly("지역");
    }

    @Test
    void 지역_매핑이_없는_정책은_전국으로_보고_제외하지_않는다() {
        Policy policy = newPolicy(1L, "지역 매핑 없는 정책");
        setFields(policy, null, null, "0013010", "0049010", "주거", "전세");
        givenCandidates(List.of(policy), Map.of());

        List<RecommendedPolicyResponse> result = recommend();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isZero();
        assertThat(result.get(0).matchedAxes()).isEmpty();
    }

    @Test
    void 전국_정책은_지역_가점을_받지_않는다() {
        // 전국은 "지역 제한을 안 둔" 것이라 나를 겨냥한 게 아니다 — 취업·학력의 제한없음과 같은 취급.
        Policy policy = newPolicy(1L, "전국 대상 정책");
        setFields(policy, null, null, "0013010", "0049010", "주거", "전세");
        ReflectionTestUtils.setField(policy, "nationwide", true);
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        List<RecommendedPolicyResponse> result = recommend();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).score()).isZero();
        assertThat(result.get(0).matchedAxes()).isEmpty();
    }

    @Test
    void 내_시도_특화_정책만_지역_가점을_받는다() {
        Policy nationwide = newPolicy(1L, "전국 대상 정책");
        setFields(nationwide, null, null, "0013010", "0049010", "주거", "전세");
        ReflectionTestUtils.setField(nationwide, "nationwide", true);
        Policy local = newPolicy(2L, "서울 전용 정책");
        setFields(local, null, null, "0013010", "0049010", "주거", "전세");
        givenCandidates(List.of(nationwide, local), Map.of(1L, List.of(SEOUL), 2L, List.of(SEOUL)));

        List<RecommendedPolicyResponse> result = recommend();

        assertThat(result).extracting(RecommendedPolicyResponse::id).containsExactly(2L, 1L);
        assertThat(result.get(0).score()).isEqualTo(18);
        assertThat(result.get(1).score()).isZero();
    }

    @Test
    void 기혼자_전용_정책은_미혼_사용자에게서_제외한다() {
        Policy policy = newPolicy(1L, "신혼부부 전세자금 지원");
        setFields(policy, null, null, "0013010", "0049010", "주거", "전세");
        // 0055001 = 기혼. 프로필은 SINGLE(0055002).
        ReflectionTestUtils.setField(policy, "maritalStatusCode", "0055001");
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 다른_전공_전용_정책은_제외한다() {
        Policy policy = newPolicy(1L, "공학인재 양성");
        setFields(policy, null, null, "0013010", "0049010", "일자리", "청년");
        // 0011005 = 공학계열. 프로필은 HUMANITIES(0011001).
        ReflectionTestUtils.setField(policy, "majorCodes", "0011005");
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 해당하지_않는_특화조건_전용_정책은_제외한다() {
        Policy policy = newPolicy(1L, "청년농업인 영농정착 지원");
        setFields(policy, null, null, "0013010", "0049010", "일자리", "청년");
        // 0014006 = 농업인. 프로필은 WOMEN(0014002).
        ReflectionTestUtils.setField(policy, "specializationCodes", "0014006");
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 연소득_상한을_넘으면_제외한다() {
        Policy policy = newPolicy(1L, "저소득 청년 지원");
        setFields(policy, null, null, "0013010", "0049010", "주거", "전세");
        // earnCndSeCd=0043002(연소득 기준), 상한 2000만원. 프로필 소득은 3000만원.
        ReflectionTestUtils.setField(policy, "incomeConditionCode", "0043002");
        ReflectionTestUtils.setField(policy, "incomeMaxAmount", 2000);
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        assertThat(recommend()).isEmpty();
    }

    @Test
    void 소득조건이_금액기준이_아니면_상한값이_있어도_제외하지_않는다() {
        Policy policy = newPolicy(1L, "소득 무관 정책");
        setFields(policy, null, null, "0013010", "0049010", "주거", "전세");
        // 0043001 = 소득 무관 — 상한값이 채워져 있어도 판정 근거가 아니다.
        ReflectionTestUtils.setField(policy, "incomeConditionCode", "0043001");
        ReflectionTestUtils.setField(policy, "incomeMaxAmount", 2000);
        givenCandidates(List.of(policy), Map.of(1L, List.of(SEOUL)));

        assertThat(recommend()).hasSize(1);
    }

    @Test
    void 점수가_높은_정책이_먼저_온다() {
        Policy low = newPolicy(1L, "지역만 맞는 정책");
        setFields(low, null, null, "0013010", "0049010", "주거", "전세");
        Policy high = newPolicy(2L, "관심분야까지 맞는 정책");
        setFields(high, null, null, "0013010", "0049010", "일자리", "전세");
        givenCandidates(List.of(low, high), Map.of(1L, List.of(SEOUL), 2L, List.of(SEOUL)));

        List<RecommendedPolicyResponse> result = recommend();

        assertThat(result).extracting(RecommendedPolicyResponse::id).containsExactly(2L, 1L);
        assertThat(result.get(0).score()).isEqualTo(38);
        assertThat(result.get(1).score()).isEqualTo(18);
    }

    private List<RecommendedPolicyResponse> recommend() {
        when(userProfileRepository.findByUserId(1L)).thenReturn(Optional.of(newProfile()));
        return recommendedPolicyService.getRecommendations(1L, null, null, null);
    }

    /** findCards 스텁 + 정책별 시도명 매핑 스텁을 한 번에 세운다. */
    private void givenCandidates(
            List<Policy> policies, Map<Long, List<String>> sidoNamesByPolicyId) {
        when(policyRepository.findCards(
                        eq(PolicyVisibility.VISIBLE),
                        any(LocalDate.class),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        isNull(),
                        any(Pageable.class)))
                .thenReturn(pageOf(policies));

        List<Long> policyIds = policies.stream().map(Policy::getId).toList();
        List<PolicyRegion> policyRegions =
                policies.stream()
                        .flatMap(
                                policy ->
                                        sidoNamesByPolicyId
                                                .getOrDefault(policy.getId(), List.of())
                                                .stream()
                                                .map(sido -> newPolicyRegion(policy, sido)))
                        .toList();
        lenient()
                .when(policyRegionRepository.findWithRegionByPolicyIdIn(policyIds))
                .thenReturn(policyRegions);
    }

    private static Page<Policy> pageOf(List<Policy> policies) {
        return new PageImpl<>(policies);
    }

    private static Policy newPolicy(Long id, String title) {
        Policy policy = BeanUtils.instantiateClass(Policy.class);
        ReflectionTestUtils.setField(policy, "id", id);
        ReflectionTestUtils.setField(policy, "policyNo", "R2026" + id);
        ReflectionTestUtils.setField(policy, "title", title);
        return policy;
    }

    private static void setFields(
            Policy policy,
            Integer minAge,
            Integer maxAge,
            String jobCodes,
            String schoolCodes,
            String category,
            String keywords) {
        ReflectionTestUtils.setField(policy, "minAge", minAge);
        ReflectionTestUtils.setField(policy, "maxAge", maxAge);
        ReflectionTestUtils.setField(policy, "jobCodes", jobCodes);
        ReflectionTestUtils.setField(policy, "schoolCodes", schoolCodes);
        ReflectionTestUtils.setField(policy, "category", category);
        ReflectionTestUtils.setField(policy, "keywords", keywords);
    }

    private static PolicyRegion newPolicyRegion(Policy policy, String sidoName) {
        return PolicyRegion.create(policy, newRegion(sidoName));
    }

    private static Region newRegion(String sidoName) {
        Region region = BeanUtils.instantiateClass(Region.class);
        ReflectionTestUtils.setField(region, "code", sidoName + "-code");
        ReflectionTestUtils.setField(region, "sidoName", sidoName);
        ReflectionTestUtils.setField(region, "name", sidoName);
        return region;
    }

    private static UserProfile newProfile() {
        User user = User.createSocialUser("google", "gid-1", "user@example.com", "닉네임");
        return UserProfile.create(
                user,
                newRegion(SEOUL),
                LocalDate.now().getYear() - 30,
                "UNEMPLOYED",
                "UNIV_GRADUATE",
                "SINGLE",
                "HUMANITIES",
                "WOMEN",
                3000,
                "일자리",
                "청년");
    }
}
