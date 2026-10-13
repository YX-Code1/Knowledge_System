package com.knowledge.base.ai.dto;

import lombok.Data;

/**
 * 向量检索命中的文章分块
 */
@Data
public class ChunkHit {

    private Long articleId;

    private String articleTitle;

    /** 文章摘要（AI 生成或人工填写），前端推荐卡片用 */
    private String articleSummary;

    private Integer chunkIndex;

    private String content;
}
