package com.bop.youthpick.user.entity;

/**
 * 온보딩 전공 선택지(선택 입력, 다중). 온통청년 {@code plcyMajorCd} 변환은 {@code YouthPolicyCodeMapper}가 담당한다.
 *
 * <p>온통청년에는 있지만 온보딩에 없는 계열(농산업)이 있어 이 목록은 부분집합이다.
 */
public enum Major {
    HUMANITIES,
    SOCIAL_SCIENCE,
    BUSINESS_ECONOMICS,
    NATURAL_SCIENCE,
    ENGINEERING,
    ARTS_PHYSICAL,
    ETC;

    /**
     * {@link EmploymentStatus#PATTERN}과 같은 이유로 손으로 적고, {@code ProfileCodePatternTest}가 일치를 강제한다.
     */
    public static final String PATTERN =
            "HUMANITIES|SOCIAL_SCIENCE|BUSINESS_ECONOMICS|NATURAL_SCIENCE|ENGINEERING|ARTS_PHYSICAL|ETC";
}
