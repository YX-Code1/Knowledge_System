package com.knowledge.base.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.knowledge.base.common.Result;
import com.knowledge.base.dto.RechargeRequest;
import com.knowledge.base.entity.User;
import com.knowledge.base.service.RechargeService;
import com.knowledge.base.vo.BalanceLogVO;
import com.knowledge.base.vo.RechargeRecordVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/recharges")
public class RechargeController {

    @Autowired
    private RechargeService rechargeService;

    /** 当前用户余额 */
    @GetMapping("/balance")
    public Result<BigDecimal> getBalance(@AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(rechargeService.getBalance(user.getId()));
    }

    /** 资金明细（余额流水） */
    @GetMapping("/logs")
    public Result<Page<BalanceLogVO>> getBalanceLogs(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(rechargeService.getBalanceLogs(user.getId(), page, size));
    }

    /** 充值（模拟支付，立即到账），返回最新余额 */
    @PostMapping
    public Result<BigDecimal> recharge(@Valid @RequestBody RechargeRequest request,
                                       @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(rechargeService.recharge(user.getId(), request.getAmount()));
    }

    /** 我的充值记录 */
    @GetMapping
    public Result<Page<RechargeRecordVO>> getMyRecharges(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int size,
                                                       @AuthenticationPrincipal User user) {
        if (user == null) {
            return Result.error("请先登录");
        }
        return Result.success(rechargeService.getMyRecharges(user.getId(), page, size));
    }
}
