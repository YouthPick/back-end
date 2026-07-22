package com.bop.youthpick.user.entity;

/**
 * 온보딩 학력 선택지. 이 목록이 서비스가 인정하는 학력 어휘의 정본이며, 온통청년 {@code schoolCd}로의 변환은 {@code
 * YouthPolicyCodeMapper}가 담당한다. 문자열로 받는 이유는 {@link EmploymentStatus}와 같다.
 */
public enum EducationLevel {
    UNDER_HS,
    HS_ENROLLED,
    HS_GRADUATE,
    UNIV_ENROLLED,
    UNIV_EXPECTED,
    UNIV_GRADUATE,
    GRAD_SCHOOL,
    ETC;

    /**
     * {@link EmploymentStatus#PATTERN}과 같은 이유로 손으로 적고, {@code ProfileCodePatternTest}가 일치를 강제한다.
     */
    public static final String PATTERN =
            "UNDER_HS|HS_ENROLLED|HS_GRADUATE|UNIV_ENROLLED|UNIV_EXPECTED|UNIV_GRADUATE|GRAD_SCHOOL|ETC";
}
