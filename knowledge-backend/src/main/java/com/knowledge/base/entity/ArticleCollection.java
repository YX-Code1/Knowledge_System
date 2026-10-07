package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 合集（类名不用 Collection，避免与 MyBatis 内置别名 java.util.Collection 冲突）
 */
@Data
@TableName("collection")
public class ArticleCollection {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 创建者ID */
    private Long userId;

    /** 合集名称 */
    private String name;

    /** 合集描述 */
    private String description;

    /** 售价，0为免费 */
    private BigDecimal price;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
