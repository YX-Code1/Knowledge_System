package com.knowledge.base.vo;

import com.knowledge.base.entity.BalanceLog;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 余额流水
 */
@Data
public class BalanceLogVO {
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

    private LocalDateTime createTime;

    public static BalanceLogVO from(BalanceLog log) {
        BalanceLogVO vo = new BalanceLogVO();
        vo.setId(log.getId());
        vo.setUserId(log.getUserId());
        vo.setChangeAmount(log.getChangeAmount());
        vo.setBalanceAfter(log.getBalanceAfter());
        vo.setType(log.getType());
        vo.setRemark(log.getRemark());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }
}
