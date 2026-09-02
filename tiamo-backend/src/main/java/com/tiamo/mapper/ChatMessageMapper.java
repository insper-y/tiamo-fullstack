package com.tiamo.mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
/**
 * 聊天消息 Mapper
 */
@Mapper
public interface ChatMessageMapper extends BaseMapper<ChatMessage> {
    /**
     * 自动建表
     */
    @Update("CREATE TABLE IF NOT EXISTS chat_message (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "from_user_id BIGINT NOT NULL COMMENT '发送者用户ID', " +
            "from_username VARCHAR(100) COMMENT '发送者用户名', " +
            "to_user_id BIGINT NOT NULL COMMENT '接收者用户ID', " +
            "to_username VARCHAR(100) COMMENT '接收者用户名', " +
            "content TEXT COMMENT '消息内容', " +
            "msg_type VARCHAR(20) DEFAULT 'text' COMMENT 'text-文本 image-图片', " +
            "is_read TINYINT DEFAULT 0 COMMENT '0-未读 1-已读', " +
            "create_time DATETIME, " +
            "read_time DATETIME, " +
            "INDEX idx_from_user (from_user_id), " +
            "INDEX idx_to_user (to_user_id), " +
            "INDEX idx_is_read (is_read), " +
            "INDEX idx_create_time (create_time)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='聊天消息表'")
    void createTableIfNotExists();

    /**
     * 检查索引是否存在
     */
    @Select("SELECT COUNT(*) FROM information_schema.STATISTICS WHERE table_schema = DATABASE() AND table_name = 'chat_message' AND index_name = #{indexName}")
    int checkIndexExists(String indexName);

    /**
     * 添加索引
     */
    @Update("ALTER TABLE chat_message ADD INDEX ${indexName} (${column})")
    void addIndex(String indexName, String column);

    /**
     * 如果索引不存在则添加
     */
    default void addIndexIfNotExists(String indexName, String column) {
        if (checkIndexExists(indexName) == 0) {
            addIndex(indexName, column);
        }
    }
}
