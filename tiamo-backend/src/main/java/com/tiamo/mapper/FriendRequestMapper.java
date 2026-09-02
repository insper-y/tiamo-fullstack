package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.FriendRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 好友请求 Mapper
 */
@Mapper
public interface FriendRequestMapper extends BaseMapper<FriendRequest> {

    /**
     * 自动建表
     */
    @Update("CREATE TABLE IF NOT EXISTS friend_request (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "from_user_id BIGINT NOT NULL COMMENT '发送者用户ID', " +
            "from_username VARCHAR(100) COMMENT '发送者用户名', " +
            "to_user_id BIGINT NOT NULL COMMENT '接收者用户ID', " +
            "to_username VARCHAR(100) COMMENT '接收者用户名', " +
            "message VARCHAR(500) COMMENT '请求消息', " +
            "status TINYINT DEFAULT 0 COMMENT '0-待处理 1-已接受 2-已拒绝', " +
            "create_time DATETIME, " +
            "handle_time DATETIME, " +
            "INDEX idx_from_user (from_user_id), " +
            "INDEX idx_to_user (to_user_id), " +
            "INDEX idx_status (status)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友请求表'")
    void createTableIfNotExists();
}
