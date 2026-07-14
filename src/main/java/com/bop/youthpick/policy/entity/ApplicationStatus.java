package com.bop.youthpick.policy.entity;

/** 정책 신청관리 상태. INTERESTED(관심)가 구 즐겨찾기 역할. 흐름: 관심 → 준비중 → 신청완료 → 종료. */
public enum ApplicationStatus {
    INTERESTED,
    PREPARING,
    SUBMITTED,
    CLOSED
}
