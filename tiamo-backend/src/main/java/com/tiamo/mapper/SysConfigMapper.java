package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.SysConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysConfigMapper extends BaseMapper<SysConfig> {

    /**
     * 创建sys_config表（如果不存在）
     */
    @Select("CREATE TABLE IF NOT EXISTS sys_config (" +
            "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
            "config_key VARCHAR(100) NOT NULL UNIQUE, " +
            "config_value TEXT, " +
            "description VARCHAR(255), " +
            "create_time DATETIME DEFAULT CURRENT_TIMESTAMP, " +
            "update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4")
    void createTableIfNotExists();
}
