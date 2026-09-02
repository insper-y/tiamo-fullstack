package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 好友请求实体
 */
@Data
@TableName("friend_request")
public class FriendRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送者用户ID */
    @TableField("from_user_id")
    private Long fromUserId;

    /** 发送者用户名 */
    @TableField("from_username")
    private String fromUsername;

    /** 接收者用户ID */
    @TableField("to_user_id")
    private Long toUserId;

    /** 接收者用户名 */
    @TableField("to_username")
    private String toUsername;

    /** 请求消息 */
    @TableField("message")
    private String message;

    /** 状态 0-待处理 1-已接受 2-已拒绝 */
    @TableField("status")
    private Integer status;

    /** 创建时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 处理时间 */
    @TableField("handle_time")
    private LocalDateTime handleTime;
}
