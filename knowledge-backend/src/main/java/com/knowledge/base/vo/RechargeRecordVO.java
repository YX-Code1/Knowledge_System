package com.knowledge.base.vo;

import com.knowledge.base.entity.RechargeRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充值记录
 */
@Data
public class RechargeRecordVO {
    private Long id;

    /** 充值用户ID */
    private Long userId;

    /** 充值金额 */
    private BigDecimal amount;

    /** 状态：1-成功 */
    private Integer status;

    private LocalDateTime createTime;

    public static RechargeRecordVO from(RechargeRecord record) {
        RechargeRecordVO vo = new RechargeRecordVO();
        vo.setId(record.getId());
        vo.setUserId(record.getUserId());
        vo.setAmount(record.getAmount());
        vo.setStatus(record.getStatus());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}
