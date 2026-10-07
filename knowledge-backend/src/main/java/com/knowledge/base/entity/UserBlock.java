package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user_block")
public class UserBlock {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 拉黑者ID */
    private Long userId;

    /** 被拉黑者ID */
    private Long blockedId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
