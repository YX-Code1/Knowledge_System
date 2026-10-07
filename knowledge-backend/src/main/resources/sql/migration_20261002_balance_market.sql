-- 迁移脚本：余额/充值/付费文章/合集
-- 警告：本脚本含 ALTER TABLE ADD COLUMN，MySQL 5.7 不支持幂等，
-- 重复执行会报 Duplicate column 错误，本脚本只能执行一次！
-- 新库请直接用 init.sql（已同步本脚本全部内容）。

USE knowledge_base;

-- 1. 用户表加余额字段
ALTER TABLE sys_user ADD COLUMN balance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '账户余额' AFTER avatar;

-- 2. 文章表加售价字段（0为免费）
ALTER TABLE article ADD COLUMN price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '售价，0为免费' AFTER summary;

-- 3. 充值记录表（日志表，无 deleted）
CREATE TABLE IF NOT EXISTS recharge_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '充值用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '充值金额',
    status TINYINT DEFAULT 1 COMMENT '状态：1-成功',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值记录表';

-- 4. 余额流水表（正负金额）
CREATE TABLE IF NOT EXISTS balance_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    change_amount DECIMAL(10,2) NOT NULL COMMENT '变动金额，正为入账负为出账',
    balance_after DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
    type VARCHAR(30) NOT NULL COMMENT '类型：RECHARGE/PURCHASE/INCOME/ADMIN',
    remark VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='余额流水表';

-- 5. 文章购买表
CREATE TABLE IF NOT EXISTS article_purchase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '成交价',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_buyer (article_id, buyer_id),
    KEY idx_buyer (buyer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章购买表';

-- 6. 合集表
CREATE TABLE IF NOT EXISTS collection (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '创建者ID',
    name VARCHAR(50) NOT NULL COMMENT '合集名称',
    description VARCHAR(200) DEFAULT NULL COMMENT '合集描述',
    price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '售价，0为免费',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合集表';

-- 7. 合集文章关联表
CREATE TABLE IF NOT EXISTS collection_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT NOT NULL COMMENT '合集ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_collection_article (collection_id, article_id),
    KEY idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合集文章关联表';

-- 8. 合集购买表
CREATE TABLE IF NOT EXISTS collection_purchase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT NOT NULL COMMENT '合集ID',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '成交价',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_collection_buyer (collection_id, buyer_id),
    KEY idx_buyer (buyer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合集购买表';
