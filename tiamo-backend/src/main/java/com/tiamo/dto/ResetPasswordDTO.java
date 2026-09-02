package com.tiamo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 重置密码请求 DTO
 */
@Data
public class ResetPasswordDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号 */
    private String phone;

    /** 邮箱 */
    private String email;

    /** 验证码 */
    private String captcha;

    /** 新密码 */
    private String newPassword;

    /** 确认新密码 */
    private String confirmPassword;
}
