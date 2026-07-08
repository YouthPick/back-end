package com.bop.youthpick.policy.entity;

/** 정책 노출 상태. deleted_at 대신 쓰는 이유: 배치가 원본 소실 정책을 숨겼다가 재등장 시 되살리는 "상태 전환"이라서. */
public enum PolicyVisibility {
    VISIBLE,
    HIDDEN
}
