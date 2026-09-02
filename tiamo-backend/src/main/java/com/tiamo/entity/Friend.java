package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友关系实体
 */
@Data
@TableName("friend")
public class Friend implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 好友用户ID */
    @TableField("friend_id")
    private Long friendId;

    /** 好友用户名 */
    @TableField("friend_username")
    private String friendUsername;

    /** 好友昵称 */
    @TableField("friend_nickname")
    private String friendNickname;

    /** 备注名 */
    @TableField("remark")
    private String remark;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;
}
