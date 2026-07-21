package com.bop.youthpick.policy.entity;

/**
 * 정책 신청관리 상태. INTERESTED(관심)가 구 즐겨찾기 역할. PREPARING(준비중)은 관심과 신청 사이 서류 준비 단계.
 *
 * <p>{@link PolicyApplication}의 {@code @Enumerated(EnumType.STRING)} 컬럼(DB엔 이 이름 그대로 저장됨)과, DB에 저장된
 * 문자열이 이 enum 상수 이름과 일치해야 한다. Bean Validation {@code @Pattern}이 필요한 곳({@code
 * PolicyApplicationCreateRequest}, {@code PolicyApplicationController.changeStatus()})은 매번 같은
 * 화이트리스트 문자열을 다시 적지 않고 {@link #VALUES_PATTERN}을 참조한다 — 상수를 추가/이름 변경할 때 이 파일 하나만 고치면 된다. 반대
 * 방향({@code @Pattern}이 못 걸러 {@code valueOf()}가 던지는 {@code IllegalArgumentException})은 {@code
 * PolicyApplicationController.parseStatus()}가 {@code CustomException(INVALID_APPLICATION_STATUS)}로
 * 바꿔줘서 500으로 새지는 않는다.
 */
public enum ApplicationStatus {
    INTERESTED,
    PREPARING,
    APPLIED,
    COMPLETED;

    /**
     * {@code @Pattern(regexp = ...)}은 어노테이션 속성이라 컴파일타임 상수만 받을 수 있어 {@code values()}로 동적 생성할 수 없다 —
     * 이 리터럴이 이 enum의 상수 이름 4개와 어긋나지 않게 유지하는 건 여전히 사람의 책임이다.
     */
    public static final String VALUES_PATTERN = "INTERESTED|PREPARING|APPLIED|COMPLETED";
}
