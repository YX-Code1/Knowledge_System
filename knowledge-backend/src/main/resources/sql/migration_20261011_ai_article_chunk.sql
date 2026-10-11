-- 迁移脚本：AI 问答索引表（文章分块 + 向量）
-- 已有数据库请手动执行本脚本（幂等，可重复执行）

USE knowledge_base;

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
