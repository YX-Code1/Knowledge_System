package com.knowledge.base.ai.dto;

import lombok.Data;

/**
 * 向量检索命中的文章分块
 */
@Data
public class ChunkHit {

    private Long articleId;

    private String articleTitle;

    private Integer chunkIndex;

    private String content;
}
