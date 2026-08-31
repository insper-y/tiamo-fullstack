package com.tiamo.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
/**
 * 系统用户实体
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {
    private static final long serialVersionUID = 1L;
    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 用户名 */
    @TableField("username")
    private String username;
    /** 密码（BCrypt加密） */
    @TableField("password")
    private String password;
    /** 昵称 */
    @TableField("nickname")
    private String nickname;
    /** 邮箱 */
    @TableField("email")
    private String email;
    /** 手机号 */
    @TableField("phone")
    private String phone;
    /** 状态 0-禁用 1-启用 */
    @TableField("status")
    private Integer status;
    /** 角色 0-普通用户 1-管理员 */
    @TableField("role")
    private Integer role;
    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
    /** 更新时间 */
    @TableField("update_time")
    private LocalDateTime updateTime;
}
