package com.tiamo.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录响应 DTO
 */
@Data
public class LoginResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT Token */
    private String token;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 昵称 */
    private String nickname;

    /** 邮箱 */
    private String email;

    public LoginResponse(String token, Long userId, String username, String nickname, String email) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.nickname = nickname;
        this.email = email;
    }
}
