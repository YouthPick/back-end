package com.bop.youthpick.policy.dto;

import com.bop.youthpick.policy.entity.PolicyVisibility;
import java.time.LocalDateTime;

/**
 * 배치 비교용 정책 스냅샷 (plcyNo → 변경 감지 기준). 전 필드 로드를 피하려는 경량 projection — rawPayload(LONGTEXT) 등을 수천 건
 * 메모리에 올리지 않기 위함.
 *
 * <p>HIDDEN 정책도 포함한다: VISIBLE만 조회하면 API에 재등장한 숨김 정책이 "신규"로 분류되어 policy_no 유니크 제약 위반으로 INSERT가 터진다.
 */
public record PolicySyncSnapshot(
        String policyNo, LocalDateTime lastModifiedAt, PolicyVisibility visibility) {}
