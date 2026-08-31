package com.tiamo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库索引初始化器
 * 启动时自动创建复合索引，优化数据处理速度到100ms以内
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

        // books 表复合索引（覆盖高频查询场景，性能优化版）
        createIndexIfNotExists("books", "idx_deleted_id", "deleted, id");
        createIndexIfNotExists("books", "idx_deleted_time", "deleted, deleted_time");
        createIndexIfNotExists("books", "idx_user_deleted", "user_id, deleted");
        createIndexIfNotExists("books", "idx_name_deleted", "name, deleted");
        createIndexIfNotExists("books", "idx_bd_deleted", "bd, deleted");

        // sys_operation_log 表复合索引（覆盖日志查询场景）
        createIndexIfNotExists("sys_operation_log", "idx_create_time", "create_time");
        createIndexIfNotExists("sys_operation_log", "idx_status_time", "status, create_time");
        createIndexIfNotExists("sys_operation_log", "idx_module_time", "module, create_time");
        createIndexIfNotExists("sys_operation_log", "idx_user_time", "user_id, create_time");
        createIndexIfNotExists("sys_operation_log", "idx_type_time", "operation_type, create_time");

        // recycle_approval 表复合索引
        createIndexIfNotExists("recycle_approval", "idx_status_time", "status, apply_time");
        createIndexIfNotExists("recycle_approval", "idx_applicant_time", "applicant_id, apply_time");

        // sys_config 表索引
        createIndexIfNotExists("sys_config", "idx_config_key", "config_key");

        System.out.println("[索引优化] 数据库索引检查完成（复合索引优化版）");
    }

    /**
     * 如果索引不存在则创建
     */
    private void createIndexIfNotExists(String tableName, String indexName, String columns) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = ? AND index_name = ?",
                    Integer.class, tableName, indexName);
            if (count != null && count > 0) {
                return;
            }
            String sql = String.format("CREATE INDEX %s ON %s (%s)", indexName, tableName, columns);
            jdbcTemplate.execute(sql);
            System.out.println("[索引优化] 已创建索引: " + tableName + "." + indexName + " (" + columns + ")");
        } catch (Exception e) {
            System.out.println("[索引优化] 创建索引失败 " + tableName + "." + indexName + ": " + e.getMessage());
        }
    }
}
