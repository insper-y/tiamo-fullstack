package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 聊天消息实体
 */
@Data
@TableName("chat_message")
public class ChatMessage implements Serializable {
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

    /** 消息内容 */
    @TableField("content")
    private String content;

    /** 消息类型 text-文本 image-图片 */
    @TableField("msg_type")
    private String msgType;

    /** 是否已读 0-未读 1-已读 */
    @TableField("is_read")
    private Integer isRead;

    /** 发送时间 */
    @TableField("create_time")
    private LocalDateTime createTime;

    /** 读取时间 */
    @TableField("read_time")
    private LocalDateTime readTime;
}
