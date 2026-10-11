package com.knowledge.base.ai.quota;

import com.knowledge.base.ai.dto.AiQuotaVO;
import com.knowledge.base.common.BusinessException;
import com.knowledge.base.service.RedisLuaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;

/**
 * AI 调用每日配额：控制 DeepSeek 接口费用（摘要/标签/问答按次计费）。
 * <p>
 * 配额 key 自带日期（Asia/Shanghai，与 MySQL serverTimezone 一致），跨天自然清零；
 * TTL 25 小时仅作兜底回收。扣减走 Redis Lua 原子「校验 + 扣减」，并发下不超扣。
 * Redis 不可用时降级放行（fail-open）：配额是成本加固手段，不应阻塞用户使用 AI。
 */
@Service
public class AiQuotaService {

    /** 每用户每日 AI 调用次数上限 */
    @Value("${ai.quota.daily-limit:30}")
    private long dailyLimit;

    /** 配额 key 兜底过期时间（秒）：key 自带日期，25 小时足够覆盖到次日 */
    private static final long QUOTA_TTL_SECONDS = 25 * 3600L;

    private static final String QUOTA_KEY_PREFIX = "ai:quota:";

    /** 与 MySQL serverTimezone=Asia/Shanghai 保持一致 */
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private RedisLuaService redisLuaService;

    /**
     * 校验并扣减今日配额，超限抛出业务异常（由全局异常处理器返回统一 JSON）。
     */
    public void checkAndConsume(Long userId, int cost) {
        Long used = redisLuaService.consumeQuota(todayKey(userId), cost, dailyLimit, QUOTA_TTL_SECONDS);
        if (used != null && used < 0) {
            throw new BusinessException("今日 AI 使用次数已达上限（" + dailyLimit + " 次），请明天再试");
        }
    }

    /** 查询今日配额使用情况（前端展示剩余次数） */
    public AiQuotaVO todayQuota(Long userId) {
        Long used = redisLuaService.getLong(todayKey(userId));
        long usedValue = used == null ? 0 : used;

        AiQuotaVO vo = new AiQuotaVO();
        vo.setUsed(usedValue);
        vo.setDailyLimit(dailyLimit);
        vo.setRemaining(Math.max(0, dailyLimit - usedValue));
        return vo;
    }

    private String todayKey(Long userId) {
        return QUOTA_KEY_PREFIX + userId + ":" + LocalDate.now(ZONE);
    }
}
