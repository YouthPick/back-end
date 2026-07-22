package com.bop.youthpick.policy.service;

import java.util.Map;
import java.util.Set;
import org.springframework.lang.Nullable;

/**
 * 온보딩 프로필이 저장하는 자체 코드({@code UNEMPLOYED} 등)를 정책이 저장하는 온통청년 코드({@code jobCd}/{@code schoolCd})로
 * 변환한다.
 *
 * <p>두 코드 체계가 갈라진 배경: 스키마 주석(V11, {@code docs/schema.sql})은 {@code
 * user_profiles.employment_status}에 jobCd를 그대로 담을 것을 전제했지만, 실제 온보딩 화면(front-end {@code
 * profileOptions.ts})은 의미 기반 자체 코드를 저장한다. 그래서 변환 없이 비교하면 취업·학력 축은 영원히 일치하지 않는다.
 *
 * <p><b>실데이터로 직접 확인한 코드</b>: {@code 0013001}=재직자(청년근로자 교통비·일자리 안심공제), {@code
 * 0013003}=미취업자(청년일자리도약장려금·국민취업지원제도), {@code 0013006}=(예비)창업자(청년창업 재정지원·청년스타트업), {@code 0049005}=대학
 * 재학(대학생 아르바이트·천원의 아침밥), {@code 0055001}=기혼(신혼부부 주거자금·출산지원금), {@code 0055002}=미혼(미혼남녀 만남 프로그램),
 * {@code 0014002}=여성(여성새로일하기센터), {@code 0014005}=장애인(장애인재능키움센터), {@code 0014006}=농업인(청년농업인 영농정착),
 * {@code 0014007}=군인(군복무 청년 상해보험), {@code 0011005}=공학계열(창의융합형 공학인재양성).
 *
 * <p><b>표준 코드표만 근거인 코드</b>(다중선택 정책뿐이라 제목으로 특정하지 못함): 나머지 전공 계열({@code 0011001}~{@code 0011004},
 * {@code 0011006}, {@code 0011008})과 특화조건 {@code 0014003}(기초생활수급자)·{@code 0014004}(한부모가정)·{@code
 * 0014008}(지역인재). 이 값들은 공식 코드표로 한 번 대조해야 하며, 틀리면 자격 필터가 <i>정당한 정책을 사용자에게서 빼앗는다</i>.
 */
final class YouthPolicyCodeMapper {

    private YouthPolicyCodeMapper() {}

    /** jobCd 제한없음 — 대상을 특정하지 않는 정책이라 "내 조건과 일치"로 보지 않는다. */
    static final String JOB_CODE_UNRESTRICTED = "0013010";

    /** schoolCd 학력무관 — 위와 동일. */
    static final String SCHOOL_CODE_UNRESTRICTED = "0049010";

    /** mrgSttsCd 제한없음. */
    static final String MARITAL_CODE_UNRESTRICTED = "0055003";

    /** plcyMajorCd 전공무관. */
    static final String MAJOR_CODE_UNRESTRICTED = "0011009";

    /** sbizCd 제한없음. */
    static final String SPECIALIZATION_CODE_UNRESTRICTED = "0014010";

    /**
     * earnCndSeCd 중 "연소득 기준" — 이 값일 때만 {@code incomeMaxAmount}가 실제 금액 상한이다(0043001=무관, 0043003=기타).
     */
    static final String INCOME_CONDITION_ANNUAL_AMOUNT = "0043002";

    private static final Map<String, String> JOB_CODE_BY_EMPLOYMENT_STATUS =
            Map.of(
                    "EMPLOYED", "0013001",
                    "SELF_EMPLOYED", "0013002",
                    "UNEMPLOYED", "0013003",
                    "FREELANCER", "0013004",
                    "STARTUP", "0013006",
                    "ETC", "0013009");

    private static final Map<String, String> SCHOOL_CODE_BY_EDUCATION_LEVEL =
            Map.of(
                    "UNDER_HS", "0049001",
                    "HS_ENROLLED", "0049002",
                    "HS_GRADUATE", "0049004",
                    "UNIV_ENROLLED", "0049005",
                    "UNIV_EXPECTED", "0049006",
                    "UNIV_GRADUATE", "0049007",
                    "GRAD_SCHOOL", "0049008",
                    "ETC", "0049009");

    private static final Map<String, String> MARITAL_CODE_BY_STATUS =
            Map.of("MARRIED", "0055001", "SINGLE", "0055002");

    private static final Map<String, String> MAJOR_CODE_BY_MAJOR =
            Map.of(
                    "HUMANITIES", "0011001",
                    "SOCIAL_SCIENCE", "0011002",
                    "BUSINESS_ECONOMICS", "0011003",
                    "NATURAL_SCIENCE", "0011004",
                    "ENGINEERING", "0011005",
                    "ARTS_PHYSICAL", "0011006",
                    "ETC", "0011008");

    private static final Map<String, String> SPECIALIZATION_CODE_BY_CONDITION =
            Map.of(
                    "WOMEN", "0014002",
                    "BASIC_LIVELIHOOD", "0014003",
                    "SINGLE_PARENT", "0014004",
                    "DISABLED", "0014005",
                    "FARMER", "0014006",
                    "VETERAN", "0014007",
                    "REGIONAL_TALENT", "0014008");

    /**
     * 매핑이 정의된 온보딩 취업상태 코드 전체. 온보딩 선택지가 늘었는데 이 표를 갱신하지 않으면 해당 사용자는 조용히 취업 축 0점이 되므로({@code
     * YouthPolicyCodeMapperTest}가 어휘 enum과의 일치를 강제한다) 테스트에서 대조용으로 쓴다.
     */
    static Set<String> supportedEmploymentStatuses() {
        return JOB_CODE_BY_EMPLOYMENT_STATUS.keySet();
    }

    /** 매핑이 정의된 온보딩 학력 코드 전체. 용도는 {@link #supportedEmploymentStatuses()}와 같다. */
    static Set<String> supportedEducationLevels() {
        return SCHOOL_CODE_BY_EDUCATION_LEVEL.keySet();
    }

    static Set<String> supportedMaritalStatuses() {
        return MARITAL_CODE_BY_STATUS.keySet();
    }

    static Set<String> supportedMajors() {
        return MAJOR_CODE_BY_MAJOR.keySet();
    }

    static Set<String> supportedSpecialConditions() {
        return SPECIALIZATION_CODE_BY_CONDITION.keySet();
    }

    @Nullable
    static String toMaritalCode(@Nullable String maritalStatus) {
        return maritalStatus == null ? null : MARITAL_CODE_BY_STATUS.get(maritalStatus);
    }

    @Nullable
    static String toMajorCode(@Nullable String major) {
        return major == null ? null : MAJOR_CODE_BY_MAJOR.get(major);
    }

    @Nullable
    static String toSpecializationCode(@Nullable String specialCondition) {
        return specialCondition == null
                ? null
                : SPECIALIZATION_CODE_BY_CONDITION.get(specialCondition);
    }

    /** 매핑되지 않는 값(코드표 개정·오타 등)은 null — 호출부가 "판정 불가"로 다루고 정책을 배제하지 않는다. */
    @Nullable
    static String toJobCode(@Nullable String employmentStatus) {
        return employmentStatus == null
                ? null
                : JOB_CODE_BY_EMPLOYMENT_STATUS.get(employmentStatus);
    }

    @Nullable
    static String toSchoolCode(@Nullable String educationLevel) {
        return educationLevel == null ? null : SCHOOL_CODE_BY_EDUCATION_LEVEL.get(educationLevel);
    }
}
