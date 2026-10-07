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

    private Integer viewCount;

    private Integer likeCount;

    private Integer commentCount;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
