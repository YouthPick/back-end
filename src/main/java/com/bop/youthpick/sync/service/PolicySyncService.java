package com.bop.youthpick.sync.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 온통청년 정책 전량 수집 배치. 상세 흐름: docs/2026-06-29-데이터수집-배치-설계.md (팀 루트 저장소) */
@Service
public class PolicySyncService {

    private static final Logger log = LoggerFactory.getLogger(PolicySyncService.class);

    /** 전량 수집 1회 실행: API 페이지 순회 → 신규/변경 선별 → upsert → 누락 처리. TODO(#15): 본 구현. 현재는 진입점만 존재한다. */
    public void runFullSync() {
        log.warn("정책 수집 배치가 아직 구현되지 않았습니다 — #15에서 구현 예정. 이번 호출은 아무 것도 하지 않습니다.");
    }
}
