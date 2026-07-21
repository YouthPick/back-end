package com.bop.youthpick.user.entity;

/** 온보딩 결혼여부 선택지(선택 입력). 온통청년 {@code mrgSttsCd} 변환은 {@code YouthPolicyCodeMapper}가 담당한다. */
public enum MaritalStatus {
    SINGLE,
    MARRIED;

    /**
     * {@link EmploymentStatus#PATTERN}과 같은 이유로 손으로 적고, {@code ProfileCodePatternTest}가 일치를 강제한다.
     */
    public static final String PATTERN = "SINGLE|MARRIED";
}
