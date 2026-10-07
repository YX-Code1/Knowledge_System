-- 迁移脚本：私信拉黑表（幂等，可重复执行）

USE knowledge_base;

CREATE TABLE IF NOT EXISTS user_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '拉黑者ID',
    blocked_id BIGINT NOT NULL COMMENT '被拉黑者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_blocked (user_id, blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信拉黑表';
