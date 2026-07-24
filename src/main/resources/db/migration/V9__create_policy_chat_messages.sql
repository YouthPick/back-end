CREATE TABLE policy_chat_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    policy_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content VARCHAR(1000) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME NULL,
    PRIMARY KEY (id),
    KEY idx_policy_chat_messages_policy_cursor (policy_id, id),
    KEY idx_policy_chat_messages_user (user_id),
    CONSTRAINT fk_policy_chat_messages_policy FOREIGN KEY (policy_id) REFERENCES policies (id),
    CONSTRAINT fk_policy_chat_messages_user FOREIGN KEY (user_id) REFERENCES users (id)
) DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='정책별 회원 대화 메시지';
