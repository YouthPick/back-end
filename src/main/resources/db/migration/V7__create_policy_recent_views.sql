-- ============================================================
-- V7 — policy_recent_views 신규 (최근 본 정책)
--   로그인 사용자가 정책 상세를 조회하면 1건씩 기록한다.
--   같은 정책을 다시 보면 행을 늘리지 않고 viewed_at만 갱신한다
--   (UNIQUE(user_id, policy_id)).
-- ============================================================

CREATE TABLE policy_recent_views (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    policy_id  BIGINT   NOT NULL,
    viewed_at  DATETIME NOT NULL COMMENT '마지막 조회 시각 (재조회 시 갱신)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_policy_recent_views_user_policy (user_id, policy_id),
    KEY idx_policy_recent_views_user_viewed (user_id, viewed_at),
    CONSTRAINT fk_policy_recent_views_user   FOREIGN KEY (user_id)   REFERENCES users (id),
    CONSTRAINT fk_policy_recent_views_policy FOREIGN KEY (policy_id) REFERENCES policies (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '최근 본 정책 (사용자별 정책 상세 조회 기록)';
