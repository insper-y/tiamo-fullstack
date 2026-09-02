package com.tiamo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 注册请求 DTO
 */
@Data
public class RegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户名 */
    private String username;

    /** 邮箱 */
    private String email;

    /** 手机号（可选） */
    private String phone;

    /** 验证码（可选，保留兼容） */
    private String captcha;

    /** 邀请码（必填，管理员生成，3分钟有效） */
    private String inviteCode;

    /** 密码 */
    private String password;

    /** 确认密码 */
    private String confirmPassword;
}
