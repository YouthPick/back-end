package com.bop.youthpick.policy.entity;

/** 정책 신청관리 상태. INTERESTED(관심)가 구 즐겨찾기 역할. PREPARING(준비중)은 관심과 신청 사이 서류 준비 단계. */
public enum ApplicationStatus {
    INTERESTED,
    PREPARING,
    APPLIED,
    COMPLETED
}
