-- 知识库系统数据库初始化脚本
-- 数据库名：knowledge_base

CREATE DATABASE IF NOT EXISTS knowledge_base DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE knowledge_base;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    email VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    balance DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '账户余额',
    role VARCHAR(20) DEFAULT 'USER' COMMENT '角色：SUPER_ADMIN/ADMIN/USER',
    status TINYINT DEFAULT 0 COMMENT '状态：0-正常 1-黑名单',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID，0为顶级',
    sort INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分类表';

-- 文章表
CREATE TABLE IF NOT EXISTS article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL COMMENT '标题',
    summary VARCHAR(500) DEFAULT NULL COMMENT '摘要',
    price DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '售价，0为免费',
    content TEXT NOT NULL COMMENT '内容',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    author_id BIGINT NOT NULL COMMENT '作者ID',
    view_count INT DEFAULT 0 COMMENT '阅读量',
    like_count INT DEFAULT 0 COMMENT '点赞数',
    comment_count INT DEFAULT 0 COMMENT '评论数',
    status TINYINT DEFAULT 1 COMMENT '状态：0-草稿 1-发布',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章表';

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '标签名称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签表';

-- 文章标签关联表
CREATE TABLE IF NOT EXISTS article_tag (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    UNIQUE KEY uk_article_tag (article_id, tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章标签关联表';

-- 评论表
CREATE TABLE IF NOT EXISTS comment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '评论用户ID',
    content TEXT NOT NULL COMMENT '评论内容',
    parent_id BIGINT DEFAULT 0 COMMENT '父评论ID，0为顶级评论',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论表';

-- 收藏表
CREATE TABLE IF NOT EXISTS favorite (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_article (user_id, article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 点赞表
CREATE TABLE IF NOT EXISTS article_like (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    user_id BIGINT NOT NULL COMMENT '点赞用户ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_article (user_id, article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章点赞表';

-- 初始化超级管理员账号（密码：admin123，BCrypt加密后）
INSERT INTO sys_user (username, password, nickname, role) VALUES
('admin', '$2a$10$.WzBZR4wQR1WszsrgPjGOOPMVgJBkV6h5GivTrmVACOC5a.aPNtdm', '超级管理员', 'SUPER_ADMIN');

-- 初始化分类
INSERT INTO category (name, parent_id, sort) VALUES
('技术', 0, 1),
('生活', 0, 2),
('学习', 0, 3),
('Java', 1, 1),
('前端', 1, 2),
('数据库', 1, 3),
('Spring Boot', 4, 1),
('Vue.js', 5, 1);

-- 初始化标签
INSERT INTO tag (name) VALUES
('Java'), ('Spring Boot'), ('Vue'), ('MySQL'), ('Redis'),
('前端'), ('后端'), ('教程'), ('笔记'), ('实战');

-- 初始化示例文章
INSERT INTO article (title, summary, content, category_id, author_id, view_count, like_count, comment_count, status) VALUES
('Spring Boot 入门教程', 'Spring Boot 快速入门指南', '# Spring Boot 入门\n\nSpring Boot 是一个基于 Spring 框架的快速开发框架。\n\n## 特点\n- 自动配置\n- 内嵌服务器\n- 简化依赖\n\n## 快速开始\n```java\n@SpringBootApplication\npublic class Application {\n    public static void main(String[] args) {\n        SpringApplication.run(Application.class, args);\n    }\n}\n```', 7, 1, 128, 0, 3, 1),
('Vue 3 组合式 API 详解', 'Vue 3 组合式 API 使用指南', '# Vue 3 组合式 API\n\nVue 3 引入了组合式 API，提供了更灵活的代码组织方式。\n\n## setup 函数\n```vue\n<script setup>\nimport { ref, computed } from "vue";\n\nconst count = ref(0);\nconst double = computed(() => count.value * 2);\n</script>\n```\n\n## 优势\n- 更好的逻辑复用\n- 更清晰的代码组织\n- 更好的 TypeScript 支持', 8, 1, 96, 0, 2, 1),
('MySQL 性能优化技巧', 'MySQL 数据库性能优化实战', '# MySQL 性能优化\n\n## 索引优化\n- 选择合适的索引类型\n- 避免索引失效\n\n## 查询优化\n- 使用 EXPLAIN 分析查询\n- 避免全表扫描\n\n## 配置优化\n```sql\ninnodb_buffer_pool_size = 1G\nquery_cache_size = 64M\n```', 6, 1, 85, 0, 1, 1);

-- 文章标签关联
INSERT INTO article_tag (article_id, tag_id) VALUES
(1, 1), (1, 2), (1, 8),
(2, 3), (2, 6), (2, 8),
(3, 4), (3, 5), (3, 9);

-- 示例评论
INSERT INTO comment (article_id, user_id, content, parent_id) VALUES
(1, 1, '很实用的教程！', 0),
(1, 1, '感谢分享', 0),
(2, 1, '组合式 API 确实很好用', 0);

-- 示例收藏
INSERT INTO favorite (article_id, user_id) VALUES
(1, 1), (2, 1);

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

-- 充值记录表（日志表，无 deleted）
CREATE TABLE IF NOT EXISTS recharge_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '充值用户ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '充值金额',
    status TINYINT DEFAULT 1 COMMENT '状态：1-成功',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充值记录表';

-- 余额流水表（正负金额）
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

-- 文章购买表
CREATE TABLE IF NOT EXISTS article_purchase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '成交价',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_article_buyer (article_id, buyer_id),
    KEY idx_buyer (buyer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章购买表';

-- 合集表
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

-- 合集文章关联表
CREATE TABLE IF NOT EXISTS collection_article (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT NOT NULL COMMENT '合集ID',
    article_id BIGINT NOT NULL COMMENT '文章ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_collection_article (collection_id, article_id),
    KEY idx_article (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合集文章关联表';

-- 合集购买表
CREATE TABLE IF NOT EXISTS collection_purchase (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    collection_id BIGINT NOT NULL COMMENT '合集ID',
    buyer_id BIGINT NOT NULL COMMENT '买家ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '成交价',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_collection_buyer (collection_id, buyer_id),
    KEY idx_buyer (buyer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合集购买表';

-- 私信拉黑表
CREATE TABLE IF NOT EXISTS user_block (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '拉黑者ID',
    blocked_id BIGINT NOT NULL COMMENT '被拉黑者ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_blocked (user_id, blocked_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='私信拉黑表';

-- AI 问答索引：每篇文章切成若干块，每块存 1024 维向量（硅基流动 bge-m3）
CREATE TABLE IF NOT EXISTS ai_article_chunk (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    article_id BIGINT NOT NULL COMMENT '文章ID',
    chunk_index INT NOT NULL COMMENT '分块序号（从0开始）',
    content TEXT NOT NULL COMMENT '分块文本内容',
    vector BLOB NOT NULL COMMENT '1024维float向量（已归一化，4KB）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    KEY idx_article_id (article_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI问答索引：文章分块及其向量';
