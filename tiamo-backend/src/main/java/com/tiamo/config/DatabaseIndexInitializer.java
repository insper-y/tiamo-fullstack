package com.tiamo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库索引初始化器
 * 启动时自动创建常用查询索引，优化数据处理速度
 */
@Component
@Order(1)
public class DatabaseIndexInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        System.out.println("[索引优化] 开始检查并创建数据库索引...");

        // sys_user 表索引
        createIndexIfNotExists("sys_user", "idx_username", "username");
        createIndexIfNotExists("sys_user", "idx_phone", "phone");
        createIndexIfNotExists("sys_user", "idx_email", "email");
        createIndexIfNotExists("sys_user", "idx_role_status", "role, status");

        // books 表索引
        createIndexIfNotExists("books", "idx_deleted", "deleted");
        createIndexIfNotExists("books", "idx_name", "name");
        createIndexIfNotExists("books", "idx_type", "type");
        createIndexIfNotExists("books", "idx_bd", "bd");
        createIndexIfNotExists("books", "idx_deleted_time", "deleted, deleted_time");

        // sys_operation_log 表索引
        createIndexIfNotExists("sys_operation_log", "idx_user_id", "user_id");
        createIndexIfNotExists("sys_operation_log", "idx_username", "username");
        createIndexIfNotExists("sys_operation_log", "idx_module", "module");
        createIndexIfNotExists("sys_operation_log", "idx_operation_type", "operation_type");
        createIndexIfNotExists("sys_operation_log", "idx_status", "status");
        createIndexIfNotExists("sys_operation_log", "idx_create_time", "create_time");
        createIndexIfNotExists("sys_operation_log", "idx_module_type_time", "module, operation_type, create_time");
        createIndexIfNotExists("sys_operation_log", "idx_user_time", "user_id, create_time");

        // sys_config 表索引
        createIndexIfNotExists("sys_config", "idx_config_key", "config_key");

        System.out.println("[索引优化] 数据库索引检查完成");
    }

    /**
     * 如果索引不存在则创建
     */
    private void createIndexIfNotExists(String tableName, String indexName, String columns) {
        try {
            // 检查索引是否存在
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, tableName, indexName);
            if (count != null && count > 0) {
                return; // 索引已存在
            }
            // 创建索引
            String sql = String.format("CREATE INDEX %s ON %s (%s)", indexName, tableName, columns);
            jdbcTemplate.execute(sql);
            System.out.println("[索引优化] 已创建索引: " + tableName + "." + indexName + " (" + columns + ")");
        } catch (Exception e) {
            System.out.println("[索引优化] 创建索引失败 " + tableName + "." + indexName + ": " + e.getMessage());
        }
    }
}
