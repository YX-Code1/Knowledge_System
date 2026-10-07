package com.knowledge.base.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.entity.BalanceLog;
import com.knowledge.base.entity.RechargeRecord;
import com.knowledge.base.entity.User;
import com.knowledge.base.mapper.BalanceLogMapper;
import com.knowledge.base.mapper.RechargeRecordMapper;
import com.knowledge.base.mapper.UserMapper;
import com.knowledge.base.service.RechargeService;
import com.knowledge.base.vo.BalanceLogVO;
import com.knowledge.base.vo.RechargeRecordVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class RechargeServiceImpl implements RechargeService {

    @Autowired
    private RechargeRecordMapper rechargeRecordMapper;

    @Autowired
    private BalanceLogMapper balanceLogMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public BigDecimal recharge(Long userId, BigDecimal amount) {
        userMapper.addBalance(userId, amount);

        RechargeRecord record = new RechargeRecord();
        record.setUserId(userId);
        record.setAmount(amount);
        record.setStatus(1);
        rechargeRecordMapper.insert(record);

        BigDecimal balanceAfter = userMapper.selectById(userId).getBalance();

        BalanceLog log = new BalanceLog();
        log.setUserId(userId);
        log.setChangeAmount(amount);
        log.setBalanceAfter(balanceAfter);
        log.setType("RECHARGE");
        log.setRemark("充值");
        balanceLogMapper.insert(log);

        return balanceAfter;
    }

    @Override
    public Page<RechargeRecordVO> getMyRecharges(Long userId, int page, int size) {
        Page<RechargeRecord> result = rechargeRecordMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<RechargeRecord>()
                        .eq(RechargeRecord::getUserId, userId)
                        .orderByDesc(RechargeRecord::getCreateTime));
        return (Page<RechargeRecordVO>) result.convert(RechargeRecordVO::from);
    }

    @Override
    public BigDecimal getBalance(Long userId) {
        User user = userMapper.selectById(userId);
        return user != null ? user.getBalance() : BigDecimal.ZERO;
    }

    @Override
    public Page<BalanceLogVO> getBalanceLogs(Long userId, int page, int size) {
        Page<BalanceLog> result = balanceLogMapper.selectPage(new Page<>(page, size),
                new LambdaQueryWrapper<BalanceLog>()
                        .eq(BalanceLog::getUserId, userId)
                        .orderByDesc(BalanceLog::getCreateTime));
        return (Page<BalanceLogVO>) result.convert(BalanceLogVO::from);
    }
}
