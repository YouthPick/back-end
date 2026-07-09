-- ============================================================
-- V2 — policies.missing_count 추가 (2026-07-08 팀 결정)
--   수집에서 안 보인 연속 횟수. 3회 도달 시 visibility=HIDDEN,
--   다시 보이면 0 리셋 + VISIBLE (즉시 숨김 금지 — 기획 §6)
--   ※ 건별 변경/파싱에러 기록(policy_sync_events)은 소비자(알림·
--     품질 리포트)가 생기는 시점에 별도 마이그레이션으로 추가한다.
--     그전까지 파싱 실패는 log.warn으로만 남긴다.
-- ============================================================

ALTER TABLE policies
    ADD COLUMN missing_count INT NOT NULL DEFAULT 0
        COMMENT '연속 누락 횟수 — 3회 도달 시 HIDDEN, 재등장 시 0 리셋'
        AFTER visibility;