package com.bop.youthpick.user.entity;

/**
 * 온보딩 특화조건 선택지(선택 입력, 다중). 온통청년 {@code sbizCd} 변환은 {@code YouthPolicyCodeMapper}가 담당한다.
 *
 * <p>온통청년에는 있지만 사용자가 주장할 수 없는 값(중소기업·기타)이 있어 이 목록은 부분집합이다.
 */
public enum SpecialCondition {
    WOMEN,
    BASIC_LIVELIHOOD,
    SINGLE_PARENT,
    DISABLED,
    FARMER,
    VETERAN,
    REGIONAL_TALENT;

    /**
     * {@link EmploymentStatus#PATTERN}과 같은 이유로 손으로 적고, {@code ProfileCodePatternTest}가 일치를 강제한다.
     */
    public static final String PATTERN =
            "WOMEN|BASIC_LIVELIHOOD|SINGLE_PARENT|DISABLED|FARMER|VETERAN|REGIONAL_TALENT";
}
