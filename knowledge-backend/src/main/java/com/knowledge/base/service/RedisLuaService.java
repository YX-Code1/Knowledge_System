package com.knowledge.base.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

/**
 * Redis Lua 脚本服务：把「读-判-写」多步操作压进 Redis 单线程原子执行，
 * 供限流计数、AI 配额等无状态计数场景复用。
 * <p>
 * 降级策略（fail-open）：脚本执行失败统一返回 null 并记告警，
 * 限流/配额属于加固手段，Redis 不可用不应阻塞登录、发送验证码、AI 调用等主流程。
 * 调用方拿到 null 自行跳过限制即可。
 */
@Slf4j
@Service
public class RedisLuaService {

    /** 原子自增计数脚本（登录失败限流 / 验证码防爆破 / IP 限流 / AI 提问频控共用） */
    private final DefaultRedisScript<Long> counterIncrScript;

    /** 原子校验并扣减配额脚本（AI 每日配额） */
    private final DefaultRedisScript<Long> quotaConsumeScript;

    private final StringRedisTemplate redisTemplate;

    public RedisLuaService(StringRedisTemplate redisTemplate,
                           @Value("classpath:lua/counter_incr.lua") Resource counterScript,
                           @Value("classpath:lua/quota_consume.lua") Resource quotaScript) {
        this.redisTemplate = redisTemplate;
        this.counterIncrScript = loadScript(counterScript);
        this.quotaConsumeScript = loadScript(quotaScript);
    }

    /**
     * 原子自增计数并管理过期时间。
     *
     * @param key            计数 key
     * @param ttlSeconds     过期秒数
     * @param renewEveryTime true = 每次自增都续期（滑动窗口）；false = 仅首次自增时设置（固定窗口）
     * @return 自增后的计数值；Redis 不可用时返回 null（调用方 fail-open）
     */
    public Long increment(String key, long ttlSeconds, boolean renewEveryTime) {
        return executeSafely(counterIncrScript, key,
                String.valueOf(ttlSeconds), renewEveryTime ? "1" : "0");
    }

    /**
     * 原子校验并扣减配额。
     *
     * @param key        配额 key
     * @param cost       本次消耗量
     * @param limit      配额上限
     * @param ttlSeconds 过期秒数（仅首次写入时设置）
     * @return 扣减后的已用量；配额不足返回 -1；Redis 不可用时返回 null（调用方 fail-open）
     */
    public Long consumeQuota(String key, long cost, long limit, long ttlSeconds) {
        return executeSafely(quotaConsumeScript, key,
                String.valueOf(cost), String.valueOf(limit), String.valueOf(ttlSeconds));
    }

    /**
     * 读取计数/配额当前值（仅用于展示，不做扣减）。
     *
     * @return 值，key 不存在返回 0；Redis 不可用时返回 null
     */
    public Long getLong(String key) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            return value == null ? 0L : Long.parseLong(value);
        } catch (Exception e) {
            log.warn("Redis Lua 服务读取失败，降级处理：{}", e.getMessage());
            return null;
        }
    }

    /** 执行 Lua 脚本，Redis 异常统一降级返回 null（fail-open，见类注释） */
    private Long executeSafely(RedisScript<Long> script, String key, String... args) {
        try {
            return redisTemplate.execute(script, List.of(key), (Object[]) args);
        } catch (Exception e) {
            log.warn("Redis Lua 脚本执行失败，降级处理：{}", e.getMessage());
            return null;
        }
    }

    /** 启动时读取脚本文本（资源缺失立即失败，而不是等到首次调用） */
    private DefaultRedisScript<Long> loadScript(Resource resource) {
        try {
            return new DefaultRedisScript<>(new ResourceScriptSource(resource).getScriptAsString(), Long.class);
        } catch (IOException e) {
            throw new IllegalStateException("Lua 脚本加载失败: " + resource, e);
        }
    }
}
