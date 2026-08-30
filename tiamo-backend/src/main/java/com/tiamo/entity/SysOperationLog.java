package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 操作日志实体
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作人ID */
    @TableField("user_id")
    private Long userId;

    /** 操作人用户名 */
    @TableField("username")
    private String username;

    /** 操作模块 */
    @TableField("module")
    private String module;

    /** 操作描述 */
    @TableField("description")
    private String description;

    /** 请求方法 */
    @TableField("method")
    private String method;

    /** 请求参数 */
    @TableField("params")
    private String params;

    /** 请求IP */
    @TableField("ip")
    private String ip;

    /** 请求URL */
    @TableField("url")
    private String url;

    /** 操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/QUERY/OTHER */
    @TableField("operation_type")
    private String operationType;

    /** 执行耗时(毫秒) */
    @TableField("cost_time")
    private Long costTime;

    /** 操作结果: SUCCESS/FAIL */
    @TableField("status")
    private String status;

    /** 错误信息 */
    @TableField("error_msg")
    private String errorMsg;

    /** 操作时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
