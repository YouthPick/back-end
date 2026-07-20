-- ============================================================
-- V5 — login_histories 신규 (관리자 로그인 이력 조회)
--   OAuth 로그인 성공 시 1건씩 기록한다. 실무적으로는 불변 로그지만
--   관리자 API 응답에 updated_at도 필요해 다른 엔티티와 동일하게
--   created_at/updated_at 둘 다 둔다.
-- ============================================================

CREATE TABLE login_histories (
    id         BIGINT   NOT NULL AUTO_INCREMENT,
    user_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_login_histories_user FOREIGN KEY (user_id) REFERENCES users (id),
    KEY idx_login_histories_user (user_id),
    KEY idx_login_histories_created (created_at)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT '로그인 이력 (관리자 조회용)';
