package com.knowledge.base.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.vo.BalanceLogVO;
import com.knowledge.base.vo.RechargeRecordVO;

import java.math.BigDecimal;

public interface RechargeService {
    /** 充值，立即到账，返回最新余额 */
    BigDecimal recharge(Long userId, BigDecimal amount);
    Page<RechargeRecordVO> getMyRecharges(Long userId, int page, int size);
    BigDecimal getBalance(Long userId);
    Page<BalanceLogVO> getBalanceLogs(Long userId, int page, int size);
}
