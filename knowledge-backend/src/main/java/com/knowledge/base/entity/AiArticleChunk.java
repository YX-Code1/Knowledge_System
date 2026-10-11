package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 问答索引：文章分块及其向量。
 * 纯派生数据（由文章内容重建），无 deleted 字段——重建时物理删除。
 */
@Data
@TableName("ai_article_chunk")
public class AiArticleChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long articleId;

    private Integer chunkIndex;

    private String content;

    /** 1024 维 float 向量（已归一化），BLOB 存储 */
    private byte[] vector;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
