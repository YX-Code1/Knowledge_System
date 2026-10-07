package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String summary;

    /** 售价，0为免费 */
    private java.math.BigDecimal price;

    private String content;

    private Long categoryId;

    private Long authorId;

    /**
     * 阅读量/点赞数/评论数：仅由 ArticleMapper 的原子 SQL 维护，
     * updateById 不回写，防止编辑文章时把期间的并发增量覆盖掉
     */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer viewCount;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer likeCount;

    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Integer commentCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
