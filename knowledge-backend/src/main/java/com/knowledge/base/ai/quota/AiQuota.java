package com.knowledge.base.ai.quota;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI 调用配额注解：标注在 AI 接口上，由 {@link AiQuotaAspect} 切面在进入业务逻辑前
 * 原子扣减每用户每日配额（Redis Lua「校验 + 扣减」），超限抛业务异常。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AiQuota {

    /** 单次调用消耗的配额点数，默认 1 */
    int cost() default 1;
}
