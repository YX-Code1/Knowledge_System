package com.knowledge.base.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("balance_log")
public class BalanceLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 变动金额，正为入账负为出账 */
    private BigDecimal changeAmount;

    /** 变动后余额 */
    private BigDecimal balanceAfter;

    /** 类型：RECHARGE-充值 PURCHASE-购买支出 INCOME-售出收入 ADMIN-管理员调整 */
    private String type;

    /** 备注 */
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
