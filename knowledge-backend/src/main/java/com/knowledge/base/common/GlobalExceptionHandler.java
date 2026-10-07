package com.knowledge.base.common;

import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理：
 * 参数校验失败等异常若不在此捕获，会转发到 /error，被安全过滤器拦成 401，
 * 导致前端误判登录过期而清除 token。这里统一转成 Result 返回。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 参数校验失败：返回第一条字段校验错误信息 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(FieldError::getDefaultMessage)
                .orElse("参数校验失败");
        return Result.error(message);
    }

    /** 请求体 JSON 解析失败 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return Result.error("请求参数格式错误");
    }
}
