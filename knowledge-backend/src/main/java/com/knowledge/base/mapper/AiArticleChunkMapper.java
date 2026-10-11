package com.knowledge.base.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.knowledge.base.entity.AiArticleChunk;
import org.apache.ibatis.annotations.Delete;

public interface AiArticleChunkMapper extends BaseMapper<AiArticleChunk> {

    /** 全量清空索引（重建索引时使用，物理删除） */
    @Delete("DELETE FROM ai_article_chunk")
    int clearAll();
}
