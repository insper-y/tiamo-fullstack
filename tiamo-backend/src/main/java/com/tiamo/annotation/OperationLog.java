package com.tiamo.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 标注在 Controller 方法上，自动记录操作日志
 *
 * 用法:
 *   @OperationLog(module = "用户管理", description = "用户登录", operationType = "LOGIN")
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperationLog {

    /** 操作模块 */
    String module() default "";

    /** 操作描述 */
    String description() default "";

    /** 操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/QUERY/OTHER */
    String operationType() default "OTHER";
}
