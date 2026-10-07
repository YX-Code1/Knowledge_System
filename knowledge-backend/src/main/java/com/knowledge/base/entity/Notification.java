package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("notification")
public class Notification {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 接收通知的用户ID */
    private Long userId;

    /** 通知类型，如 CATEGORY_CHANGED（分类新增子分类） */
    private String type;

    /** 通知内容 */
    private String content;

    /** 已读状态：0-未读 1-已读 */
    private Integer isRead;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
