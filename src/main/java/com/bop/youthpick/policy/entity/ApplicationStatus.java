package com.bop.youthpick.policy.entity;

/**
 * 정책 신청관리 상태. INTERESTED(관심)가 구 즐겨찾기 역할. PREPARING(준비중)은 관심과 신청 사이 서류 준비 단계.
 *
 * <p>이 enum 상수 이름 4개는 코드 곳곳에 문자열로 하드코딩되어 있어 같이 맞춰야 한다: {@link PolicyApplication}의
 * {@code @Enumerated(EnumType.STRING)} 컬럼(DB엔 이 이름 그대로 저장됨), {@code
 * PolicyApplicationRegisterRequest}의 {@code @Pattern(regexp =
 * "INTERESTED|PREPARING|APPLIED|COMPLETED")}, {@code PolicyApplicationController.changeStatus()}의
 * 동일한 {@code @Pattern}. 상수를 추가/이름 변경할 때 이 세 곳을 빠뜨리면 유효한 값인데도 400으로 막힐 수 있다 — 다만 반대
 * 방향({@code @Pattern}이 못 걸러 {@code valueOf()}가 던지는 {@code IllegalArgumentException})은 {@code
 * PolicyApplicationController.parseStatus()}가 {@code CustomException(INVALID_APPLICATION_STATUS)}로
 * 바꿔줘서 500으로 새지는 않는다.
 */
public enum ApplicationStatus {
    INTERESTED,
    PREPARING,
    APPLIED,
    COMPLETED
}
