package com.bop.youthpick.policy.service;

import com.bop.youthpick.policy.dto.RecommendedPolicyResponse;
import com.bop.youthpick.policy.entity.Policy;
import com.bop.youthpick.policy.entity.PolicyVisibility;
import com.bop.youthpick.policy.repository.PolicyRegionRepository;
import com.bop.youthpick.policy.repository.PolicyRepository;
import com.bop.youthpick.user.entity.UserProfile;
import com.bop.youthpick.user.exception.UserError;
import com.bop.youthpick.user.exception.UserException;
import com.bop.youthpick.user.repository.UserProfileRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 로그인 사용자의 온보딩 프로필과 정책 자격조건을 매칭해 맞춤정책을 조회한다.
 *
 * <p><b>자격(하드 필터)</b> — 정책이 조건을 <i>명시했는데</i> 사용자가 그 밖이면 목록에서 뺀다: 나이·지역·취업·학력·결혼여부·전공·특화조건·소득. 다른 시도
 * 전용이거나 재직자 전용인 정책을 미취업 사용자에게 "맞춤"으로 보여주는 건 추천이 아니라 오답이기 때문이다. 조건을 명시하지 않았거나(제한없음) 사용자가 값을 입력하지 않아
 * 판정할 수 없으면 빼지 않는다.
 *
 * <p><b>적합도(점수)</b> — 10축 합계 100점: 분야20·지역18·특화조건12·취업12·키워드10·학력8·전공7·결혼여부5·나이5·소득3. 배점은 "그 축이
 * 일치했을 때 사용자를 얼마나 좁게 특정하는가"로 정했다. 모든 축이 같은 규칙을 따른다 — "자격 통과"에는 주지 않고 정책이 조건을 명시했고 사용자가 거기 해당할 때만
 * 준다. 제한없음(전국·학력무관 등)은 겨냥이 아니므로 0점이다.
 *
 * <p>자격에만 쓰이고 점수에는 없는 축은 없다 — 자격 조건 8축이 모두 점수에도 참여하며, 분야·키워드는 반대로 자격에는 쓰지 않는다(관심사가 달라도 신청은 가능하므로 제외
 * 조건이 되면 안 된다).
 *
 * <p>취업·학력은 프로필과 정책의 코드 체계가 달라 {@link YouthPolicyCodeMapper}로 변환한 뒤 비교한다.
 */
@Service
@RequiredArgsConstructor
public class RecommendedPolicyService {

    private static final String NATIONWIDE_REGION = "전국";

    // 배점 원칙: 그 축이 일치했을 때 사용자를 얼마나 좁게 특정하는가. 좁게 특정할수록 높다.
    // 합계 100점이지만 10개 축이 모두 "명시 + 일치"하는 정책은 현실에 거의 없어, 절대값보다 상대 순위가 의미를 갖는다.
    private static final int CATEGORY_WEIGHT = 20; // 사용자가 직접 고른 관심분야
    private static final int REGION_WEIGHT = 18; // 내 시도 전용 정책
    private static final int SPECIALIZATION_WEIGHT = 12; // 여성·장애인·농업인 등 가장 좁은 대상
    private static final int EMPLOYMENT_WEIGHT = 12; // 청년정책의 핵심 구분축
    private static final int KEYWORD_WEIGHT = 10; // 사용자 선택이지만 문자열 교집합이라 노이즈 있음
    private static final int EDUCATION_WEIGHT = 8;
    private static final int MAJOR_WEIGHT = 7;
    private static final int MARITAL_WEIGHT = 5; // 인구를 절반씩 가르는 넓은 조건
    private static final int AGE_WEIGHT = 5; // 청년정책 대부분이 청년 범위를 명시 — 특정력이 거의 없다
    private static final int INCOME_WEIGHT = 3; // "상한 이하" 범위 조건이라 가장 넓다

    private final PolicyRepository policyRepository;
    private final PolicyRegionRepository policyRegionRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public List<RecommendedPolicyResponse> getRecommendations(
            Long userId,
            @Nullable String category,
            @Nullable String keyword,
            @Nullable String region) {
        UserProfile profile =
                userProfileRepository
                        .findByUserId(userId)
                        .orElseThrow(() -> new UserException(UserError.PROFILE_NOT_FOUND));

        String categoryFilter = trimToNull(category);
        String keywordPattern = toLikePattern(keyword);
        String regionFilter = trimToNull(region);
        // '전국'은 지역 드롭다운의 "지역 무관" 선택지라 시도명 필터를 걸지 않는다(PolicyService와 동일 규약).
        String sidoName = NATIONWIDE_REGION.equals(regionFilter) ? null : regionFilter;

        Page<Policy> candidates =
                policyRepository.findCards(
                        PolicyVisibility.VISIBLE,
                        LocalDate.now(),
                        categoryFilter,
                        keywordPattern,
                        sidoName,
                        null,
                        null,
                        Pageable.unpaged());

        List<Long> policyIds = candidates.getContent().stream().map(Policy::getId).toList();
        Map<Long, List<String>> sidoNamesByPolicyId =
                policyIds.isEmpty()
                        ? Map.of()
                        : policyRegionRepository.findWithRegionByPolicyIdIn(policyIds).stream()
                                .collect(
                                        Collectors.groupingBy(
                                                pr -> pr.getPolicy().getId(),
                                                Collectors.mapping(
                                                        pr -> pr.getRegion().getSidoName(),
                                                        Collectors.toList())));

        int age = LocalDate.now().getYear() - profile.getBirthYear();
        String userSidoName = profile.getRegion().getSidoName();
        String userJobCode = YouthPolicyCodeMapper.toJobCode(profile.getEmploymentStatus());
        String userSchoolCode = YouthPolicyCodeMapper.toSchoolCode(profile.getEducationLevel());
        UserMatchProfile user =
                new UserMatchProfile(
                        age,
                        userSidoName,
                        userJobCode,
                        userSchoolCode,
                        YouthPolicyCodeMapper.toMaritalCode(profile.getMerryStatus()),
                        mapAll(profile.getMajor(), YouthPolicyCodeMapper::toMajorCode),
                        mapAll(
                                profile.getSpecialCondition(),
                                YouthPolicyCodeMapper::toSpecializationCode),
                        profile.getIncome(),
                        splitToSet(profile.getCategories()),
                        splitToSet(profile.getKeywords()));

        List<RecommendedPolicyResponse> recommendations = new ArrayList<>();
        for (Policy policy : candidates.getContent()) {
            List<String> provinces =
                    sidoNamesByPolicyId.getOrDefault(policy.getId(), List.of()).stream()
                            .distinct()
                            .sorted()
                            .toList();
            if (!isEligible(policy, provinces, user)) {
                continue;
            }
            recommendations.add(score(policy, provinces, user));
        }

        // 점수 내림차순, 동점이면 id 내림차순(최신순).
        recommendations.sort(
                Comparator.comparingInt(RecommendedPolicyResponse::score)
                        .thenComparingLong(RecommendedPolicyResponse::id)
                        .reversed());
        return recommendations;
    }

    /**
     * 자격 판정에 필요한 사용자 값 묶음. 코드 변환에 실패했거나 사용자가 입력하지 않은 항목은 null/빈 값이며, 그 경우 판정을 포기하고 통과시킨다.
     *
     * @param majorCodes 전공은 다중 선택이라 목록. 특화조건도 같다.
     */
    private record UserMatchProfile(
            int age,
            String sidoName,
            @Nullable String jobCode,
            @Nullable String schoolCode,
            @Nullable String maritalCode,
            Set<String> majorCodes,
            Set<String> specializationCodes,
            @Nullable Integer income,
            Set<String> categories,
            Set<String> keywords) {}

    /** 정책이 명시한 자격조건 중 하나라도 사용자가 벗어나면 추천 대상이 아니다. */
    private static boolean isEligible(
            Policy policy, List<String> provinces, UserMatchProfile user) {
        return withinAgeRange(policy, user.age())
                && coversRegion(provinces, user.sidoName())
                && satisfiesCode(
                        policy.getJobCodes(),
                        user.jobCode(),
                        YouthPolicyCodeMapper.JOB_CODE_UNRESTRICTED)
                && satisfiesCode(
                        policy.getSchoolCodes(),
                        user.schoolCode(),
                        YouthPolicyCodeMapper.SCHOOL_CODE_UNRESTRICTED)
                && satisfiesCode(
                        policy.getMaritalStatusCode(),
                        user.maritalCode(),
                        YouthPolicyCodeMapper.MARITAL_CODE_UNRESTRICTED)
                && satisfiesAnyCode(
                        policy.getMajorCodes(),
                        user.majorCodes(),
                        YouthPolicyCodeMapper.MAJOR_CODE_UNRESTRICTED)
                && satisfiesAnyCode(
                        policy.getSpecializationCodes(),
                        user.specializationCodes(),
                        YouthPolicyCodeMapper.SPECIALIZATION_CODE_UNRESTRICTED)
                && withinIncomeLimit(policy, user.income());
    }

    /**
     * 연소득 상한을 넘으면 신청 자체가 불가하므로 제외한다. 금액 상한은 {@code earnCndSeCd}가 "연소득 기준"일 때만 유효하다 — 무관·기타 조건에서는
     * {@code incomeMaxAmount}가 채워져 있어도 판정 근거가 아니다. 소득 미입력(null)은 판정하지 않는다("소득 무관"과 "미설정"을 저장 단계에서
     * 구분하지 못하기 때문).
     */
    private static boolean withinIncomeLimit(Policy policy, @Nullable Integer userIncome) {
        if (userIncome == null
                || !YouthPolicyCodeMapper.INCOME_CONDITION_ANNUAL_AMOUNT.equals(
                        policy.getIncomeConditionCode())) {
            return true;
        }
        Integer maxAmount = policy.getIncomeMaxAmount();
        return maxAmount == null || maxAmount <= 0 || userIncome <= maxAmount;
    }

    /** min/maxAge가 0 또는 NULL이면 제한없음이라 항상 통과한다. */
    private static boolean withinAgeRange(Policy policy, int age) {
        Integer minAge = policy.getMinAge();
        Integer maxAge = policy.getMaxAge();
        if (minAge != null && minAge > 0 && age < minAge) {
            return false;
        }
        return maxAge == null || maxAge <= 0 || age <= maxAge;
    }

    /** 지역 매핑이 없으면 지역 제한이 없는 것으로 본다(프론트 {@code provincesToLabel}이 '전국'으로 표시하는 규칙과 동일). */
    private static boolean coversRegion(List<String> provinces, String userSidoName) {
        return provinces.isEmpty() || provinces.contains(userSidoName);
    }

    /**
     * 정책이 코드를 명시했고 제한없음도 아닌데 사용자 코드가 빠져 있으면 자격 미달. 사용자 코드를 변환하지 못했으면(null) 판정을 포기하고 통과시킨다 — 코드표 불일치
     * 때문에 후보를 통째로 날리는 것보다 낫다.
     */
    private static boolean satisfiesCode(
            @Nullable String policyCodes, @Nullable String userCode, String unrestrictedCode) {
        Set<String> codes = splitToSet(policyCodes);
        if (codes.isEmpty() || codes.contains(unrestrictedCode) || userCode == null) {
            return true;
        }
        return codes.contains(userCode);
    }

    /**
     * 자격을 통과한 정책의 적합도를 매긴다. 모든 축이 같은 규칙을 따른다 — "자격 통과"에는 주지 않고, 정책이 <b>조건을 명시했고 사용자가 거기 해당할 때만</b>
     * 준다. 제한없음(전국·학력무관 등)은 사용자를 겨냥한 게 아니므로 0점이다.
     */
    private static RecommendedPolicyResponse score(
            Policy policy, List<String> provinces, UserMatchProfile user) {
        int total = 0;
        List<String> matchedAxes = new ArrayList<>();

        if (containsToken(user.categories(), policy.getCategory())
                || containsToken(user.categories(), policy.getMiddleCategory())) {
            total += CATEGORY_WEIGHT;
            matchedAxes.add("분야");
        }
        if (!policy.isNationwide() && !provinces.isEmpty()) {
            total += REGION_WEIGHT;
            matchedAxes.add("지역");
        }
        if (matchesAnyCode(
                policy.getSpecializationCodes(),
                user.specializationCodes(),
                YouthPolicyCodeMapper.SPECIALIZATION_CODE_UNRESTRICTED)) {
            total += SPECIALIZATION_WEIGHT;
            matchedAxes.add("특화조건");
        }
        if (matchesCode(
                policy.getJobCodes(),
                user.jobCode(),
                YouthPolicyCodeMapper.JOB_CODE_UNRESTRICTED)) {
            total += EMPLOYMENT_WEIGHT;
            matchedAxes.add("취업");
        }
        Set<String> policyKeywords = splitToSet(policy.getKeywords());
        if (!user.keywords().isEmpty()
                && !policyKeywords.isEmpty()
                && !Collections.disjoint(user.keywords(), policyKeywords)) {
            total += KEYWORD_WEIGHT;
            matchedAxes.add("키워드");
        }
        if (matchesCode(
                policy.getSchoolCodes(),
                user.schoolCode(),
                YouthPolicyCodeMapper.SCHOOL_CODE_UNRESTRICTED)) {
            total += EDUCATION_WEIGHT;
            matchedAxes.add("학력");
        }
        if (matchesAnyCode(
                policy.getMajorCodes(),
                user.majorCodes(),
                YouthPolicyCodeMapper.MAJOR_CODE_UNRESTRICTED)) {
            total += MAJOR_WEIGHT;
            matchedAxes.add("전공");
        }
        if (matchesCode(
                policy.getMaritalStatusCode(),
                user.maritalCode(),
                YouthPolicyCodeMapper.MARITAL_CODE_UNRESTRICTED)) {
            total += MARITAL_WEIGHT;
            matchedAxes.add("결혼여부");
        }
        if (statesAgeLimit(policy)) {
            total += AGE_WEIGHT;
            matchedAxes.add("나이");
        }
        if (statesIncomeLimit(policy) && user.income() != null) {
            total += INCOME_WEIGHT;
            matchedAxes.add("소득");
        }

        return RecommendedPolicyResponse.of(policy, provinces, total, matchedAxes);
    }

    /** 다중 선택(전공·특화조건)용 가점 판정. 제한없음이면 겨냥이 아니고, 사용자가 값을 안 넣었으면 판정할 게 없다. */
    private static boolean matchesAnyCode(
            @Nullable String policyCodes, Set<String> userCodes, String unrestrictedCode) {
        if (userCodes.isEmpty()) {
            return false;
        }
        Set<String> codes = splitToSet(policyCodes);
        return !codes.contains(unrestrictedCode) && !Collections.disjoint(codes, userCodes);
    }

    /**
     * 소득 가점은 정책이 실제 금액 상한을 건 경우에만 준다. 상한을 넘는 사용자는 이미 자격에서 걸러졌으므로, 여기 도달했다면 상한 이내라는 뜻이다 — 즉 "저소득 대상
     * 정책에 내가 해당"이라는 겨냥 신호다.
     */
    private static boolean statesIncomeLimit(Policy policy) {
        if (!YouthPolicyCodeMapper.INCOME_CONDITION_ANNUAL_AMOUNT.equals(
                policy.getIncomeConditionCode())) {
            return false;
        }
        Integer maxAmount = policy.getIncomeMaxAmount();
        return maxAmount != null && maxAmount > 0;
    }

    /** 나이 가점은 정책이 나이 조건을 실제로 명시한 경우에만 준다(제한없음은 "내 조건과 일치"가 아니다). */
    private static boolean statesAgeLimit(Policy policy) {
        Integer minAge = policy.getMinAge();
        Integer maxAge = policy.getMaxAge();
        return (minAge != null && minAge > 0) || (maxAge != null && maxAge > 0);
    }

    /** 제한없음은 가점 대상이 아니다 — 자격 판정({@link #satisfiesCode})에서는 이미 통과시킨 뒤다. */
    private static boolean matchesCode(
            @Nullable String policyCodes, @Nullable String userCode, String unrestrictedCode) {
        if (userCode == null) {
            return false;
        }
        Set<String> codes = splitToSet(policyCodes);
        return !codes.contains(unrestrictedCode) && codes.contains(userCode);
    }

    /**
     * 다중 선택(전공·특화조건)용 자격 판정. 사용자가 아무것도 고르지 않았으면 통과시킨다 — 온보딩에서 "해당 없음"과 "입력 안 함"을 구분해 저장하지 않아, 미입력을
     * 부적격으로 처리하면 정당한 후보를 잘못 지운다.
     */
    private static boolean satisfiesAnyCode(
            @Nullable String policyCodes, Set<String> userCodes, String unrestrictedCode) {
        Set<String> codes = splitToSet(policyCodes);
        if (codes.isEmpty() || codes.contains(unrestrictedCode) || userCodes.isEmpty()) {
            return true;
        }
        return !Collections.disjoint(codes, userCodes);
    }

    /** 콤마목록을 온통청년 코드 집합으로 변환한다. 변환 실패한 항목은 버린다(판정 불가). */
    private static Set<String> mapAll(
            @Nullable String commaSeparated, UnaryOperator<String> toCode) {
        return splitToSet(commaSeparated).stream()
                .map(toCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private static boolean containsToken(Set<String> userTokens, @Nullable String value) {
        String trimmed = trimToNull(value);
        return trimmed != null && userTokens.contains(trimmed);
    }

    private static Set<String> splitToSet(@Nullable String commaSeparated) {
        if (commaSeparated == null || commaSeparated.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(commaSeparated.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toSet());
    }

    @Nullable
    private static String trimToNull(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** LIKE 특수문자(%/_)와 이스케이프 문자(!)를 이스케이프하고 부분일치 패턴으로 감싼다. */
    @Nullable
    private static String toLikePattern(@Nullable String keyword) {
        String normalized = trimToNull(keyword);
        if (normalized == null) {
            return null;
        }
        String escaped = normalized.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }
}
