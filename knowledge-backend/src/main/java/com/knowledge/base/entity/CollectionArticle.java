package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("collection_article")
public class CollectionArticle {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 合集ID */
    private Long collectionId;

    /** 文章ID */
    private Long articleId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
