package com.bop.youthpick.user.entity;

/**
 * 온보딩 취업상태 선택지. 이 목록이 서비스가 인정하는 취업상태 어휘의 정본이며, 온통청년 {@code jobCd}로의 변환은 {@code
 * YouthPolicyCodeMapper}가 담당한다.
 *
 * <p>{@link UserProfile#getEmploymentStatus()}는 이 enum의 {@code name()}을 문자열로 저장한다 — 잘못된 enum 값이
 * Jackson 역직렬화 단계에서 터지지 않고 {@code C001} 입력값 오류로 내려가도록, 요청 DTO는 enum이 아니라 문자열 + {@link #PATTERN}으로
 * 받는다 (api-design 규칙).
 */
public enum EmploymentStatus {
    UNEMPLOYED,
    EMPLOYED,
    SELF_EMPLOYED,
    FREELANCER,
    STARTUP,
    ETC;

    /**
     * {@code @Pattern}은 컴파일 상수만 받으므로 상수 목록을 손으로 한 번 더 적는다. 이 문자열과 위 상수가 어긋나면 {@code
     * ProfileCodePatternTest}가 잡는다.
     */
    public static final String PATTERN = "UNEMPLOYED|EMPLOYED|SELF_EMPLOYED|FREELANCER|STARTUP|ETC";
}
