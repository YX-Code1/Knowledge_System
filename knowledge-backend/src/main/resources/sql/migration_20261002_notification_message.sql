-- 迁移脚本：新增通知表与私信表
-- 已有数据库请手动执行本脚本（幂等，可重复执行）

USE knowledge_base;

-- 通知表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '接收通知的用户ID',
    type VARCHAR(30) DEFAULT 'CATEGORY_CHANGED' COMMENT '通知类型',
    content VARCHAR(500) NOT NULL COMMENT '通知内容',
    is_read TINYINT DEFAULT 0 COMMENT '已读状态：0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    KEY idx_user_read (user_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';

-- 私信表
CREATE TABLE IF NOT EXISTS message (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL COMMENT '发送者ID',
    receiver_id BIGINT NOT NULL COMMENT '接收者ID',
    content VARCHAR(1000) NOT NULL COMMENT '消息内容',
    is_read TINYINT DEFAULT 0 COMMENT '已读状态：0-未读 1-已读',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    KEY idx_sender_receiver (sender_id, receiver_id),
    KEY idx_receiver_read (receiver_id, is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信表';
