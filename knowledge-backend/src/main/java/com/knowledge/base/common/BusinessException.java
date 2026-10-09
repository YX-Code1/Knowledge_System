package com.knowledge.base.common;

/**
 * 业务异常：Service 层业务规则不满足时抛出，message 为面向用户的中文提示。
 *
 * 由 {@link GlobalExceptionHandler} 统一捕获并转成 Result 返回，
 * Controller 不再需要 try/catch 包裹，实现「业务失败」与「系统异常」的分离：
 * 数据库报错、NPE 等内部异常走兜底分支记日志，细节不会泄露给前端。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
