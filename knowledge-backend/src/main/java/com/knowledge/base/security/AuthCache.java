package com.knowledge.base.security;

/**
 * 登录态认证信息缓存的 key 规则，供认证过滤器与用户管理服务共用。
 *
 * 缓存内容为 role|status|deleted 三个字段的拼接，
 * 用途：JWT 解析出 userId 后不再每个请求都查库，
 * 而是先读 Redis 缓存（命中则跳过数据库）。
 * 失效机制双保险：TTL 兜底 + 角色/状态变更时主动删除缓存。
 */
public final class AuthCache {

    /** 缓存有效期（秒）：安全兜底，正常靠变更时主动失效 */
    public static final long TTL_SECONDS = 60;

    private AuthCache() {
    }

    public static String key(Long userId) {
        return "auth:user:" + userId;
    }
}
