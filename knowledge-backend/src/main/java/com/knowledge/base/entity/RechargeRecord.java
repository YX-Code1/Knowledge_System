package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("recharge_record")
public class RechargeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 充值用户ID */
    private Long userId;

    /** 充值金额 */
    private BigDecimal amount;

    /** 状态：1-成功 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
