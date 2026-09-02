package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.Friend;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 好友关系 Mapper
 */
@Mapper
public interface FriendMapper extends BaseMapper<Friend> {

    /**
     * 自动建表
     */
    @Update("CREATE TABLE IF NOT EXISTS friend (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "user_id BIGINT NOT NULL COMMENT '用户ID', " +
            "friend_id BIGINT NOT NULL COMMENT '好友用户ID', " +
            "friend_username VARCHAR(100) COMMENT '好友用户名', " +
            "friend_nickname VARCHAR(100) COMMENT '好友昵称', " +
            "remark VARCHAR(100) COMMENT '备注名', " +
            "create_time DATETIME, " +
            "INDEX idx_user (user_id), " +
            "INDEX idx_friend (friend_id), " +
            "UNIQUE KEY uk_user_friend (user_id, friend_id)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='好友关系表'")
    void createTableIfNotExists();
}
