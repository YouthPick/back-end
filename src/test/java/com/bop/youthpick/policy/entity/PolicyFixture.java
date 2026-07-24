package com.bop.youthpick.policy.entity;

import java.time.LocalDateTime;

/** 테스트 공용 Policy 생성 헬퍼 — 스냅샷/업서트 검증에 필요한 필드만 채운 최소 정책. */
public final class PolicyFixture {

    private PolicyFixture() {}

    /** 파라미터 순서 = Policy 필드 선언 순서 (Policy.create 규약). */
    public static Policy policy(String policyNo, String title, LocalDateTime lastModifiedAt) {
        return Policy.create(
                policyNo,
                title,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                0,
                null,
                lastModifiedAt,
                null);
    }
}
