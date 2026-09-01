package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 系统运行日志实体类
 * 记录Controller/Service方法调用信息：类名、方法名、入参、返回值、耗时、异常堆栈、操作人
 */
@TableName("sys_run_log")
public class SysRunLog {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 类名 */
    private String className;

    /** 方法名 */
    private String methodName;

    /** 方法全限定名（类名.方法名） */
    private String fullMethod;

    /** 入参（JSON） */
    private String params;

    /** 返回值（JSON） */
    private String result;

    /** 耗时（毫秒） */
    private Long costTime;

    /** 异常信息 */
    private String exception;

    /** 异常堆栈 */
    private String exceptionStack;

    /** 调用层级：CONTROLLER / SERVICE */
    private String level;

    /** 操作人ID */
    private Long userId;

    /** 操作人用户名 */
    private String username;

    /** 客户端IP */
    private String ip;

    /** 请求URL */
    private String url;

    /** 调用状态：SUCCESS / FAIL */
    private String status;

    /** 创建时间 */
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    public String getFullMethod() { return fullMethod; }
    public void setFullMethod(String fullMethod) { this.fullMethod = fullMethod; }
    public String getParams() { return params; }
    public void setParams(String params) { this.params = params; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public Long getCostTime() { return costTime; }
    public void setCostTime(Long costTime) { this.costTime = costTime; }
    public String getException() { return exception; }
    public void setException(String exception) { this.exception = exception; }
    public String getExceptionStack() { return exceptionStack; }
    public void setExceptionStack(String exceptionStack) { this.exceptionStack = exceptionStack; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
