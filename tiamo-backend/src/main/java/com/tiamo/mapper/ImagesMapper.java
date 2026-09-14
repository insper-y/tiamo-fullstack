package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.Images;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ImagesMapper extends BaseMapper<Images> {

    @Update("CREATE TABLE IF NOT EXISTS images (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "file_name VARCHAR(255) NOT NULL, " +
            "original_name VARCHAR(255), " +
            "file_path VARCHAR(500) NOT NULL, " +
            "file_size BIGINT, " +
            "mime_type VARCHAR(100), " +
            "user_id BIGINT, " +
            "username VARCHAR(100), " +
            "create_time DATETIME, " +
            "thumbnail_path VARCHAR(500), " +
            "INDEX idx_user_id (user_id), " +
            "INDEX idx_create_time (create_time)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
    void createTable();
}
