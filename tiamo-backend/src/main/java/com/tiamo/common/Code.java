package com.tiamo.common;

/**
 * 业务状态码常量
 * 与前端 login.html 中判断的 code 完全对应
 */
public class Code {

    // 新增
    public static final Integer SAVE_OK = 20011;
    public static final Integer SAVE_ERR = 20010;

    // 删除
    public static final Integer DELETE_OK = 20021;
    public static final Integer DELETE_ERR = 20020;

    // 修改
    public static final Integer UPDATE_OK = 20031;
    public static final Integer UPDATE_ERR = 20030;

    // 查询
    public static final Integer GET_OK = 20041;
    public static final Integer GET_ERR = 20040;

    // 系统异常
    public static final Integer SYSTEM_ERR = 50000;
    public static final Integer SYSTEM_TIMEOUT_ERR = 50001;

    // 业务异常
    public static final Integer BUSINESS_ERR = 60001;
}
