package com.knowledge.base.ai.quota;

import com.knowledge.base.entity.User;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * AI 配额切面：拦截标注 {@link AiQuota} 的接口，进入业务逻辑前原子扣减配额。
 * 超限抛 {@link com.knowledge.base.common.BusinessException}，由全局异常处理器统一返回 JSON。
 */
@Aspect
@Component
public class AiQuotaAspect {

    @Autowired
    private AiQuotaService aiQuotaService;

    @Around("@annotation(aiQuota)")
    public Object around(ProceedingJoinPoint joinPoint, AiQuota aiQuota) throws Throwable {
        User user = extractUser(joinPoint.getArgs());
        if (user != null) {
            aiQuotaService.checkAndConsume(user.getId(), aiQuota.cost());
        }
        return joinPoint.proceed();
    }

    /** 从入参中取出当前登录用户（Controller 通过 @AuthenticationPrincipal 注入） */
    private User extractUser(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof User user) {
                return user;
            }
        }
        return null;
    }
}
