package com.tiamo.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * 统一捕获异常，返回标准 Result 格式，避免堆栈信息泄露到前端
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<String> handleBusinessException(BusinessException e) {
        return new Result<>(Code.BUSINESS_ERR, null, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<String> handleException(Exception e) {
        e.printStackTrace();
        String msg = "系统繁忙，请稍后重试: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        if (e.getCause() != null) {
            msg += " | Cause: " + e.getCause().getClass().getSimpleName() + " - " + e.getCause().getMessage();
        }
        return new Result<>(Code.SYSTEM_ERR, null, msg);
    }
}
