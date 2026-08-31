package com.tiamo.config;

import com.tiamo.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库初始化组件
 * 启动时自动创建sys_config表并初始化默认配置
 */
@Component
public class DatabaseInitializer implements CommandLineRunner {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysConfigService configService;

    @Override
    public void run(String... args) {
        try {
            // 创建sys_config表（如果不存在）
            String createTableSql = """
                    CREATE TABLE IF NOT EXISTS sys_config (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        config_key VARCHAR(100) NOT NULL UNIQUE,
                        config_value TEXT,
                        description VARCHAR(255),
                        create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
                        update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                    """;
            jdbcTemplate.execute(createTableSql);
            System.out.println("[数据库初始化] sys_config表已就绪");

            // 初始化默认配置
            initDefaultConfig();

        } catch (Exception e) {
            System.err.println("[数据库初始化] 失败: " + e.getMessage());
        }
    }

    private void initDefaultConfig() {
        // 日报发送时间，默认20:00
        configService.setConfigValue("email.send.time", "20:00", "日报发送时间(HH:mm)");
        // 日报邮件开关，默认开启
        configService.setConfigValue("email.send.enabled", "true", "日报邮件开关");
        // 收件邮箱
        configService.setConfigValue("email.send.to", "1301628876@qq.com", "收件邮箱");
        // 实时邮件开关，默认开启
        configService.setConfigValue("email.realtime.enabled", "true", "实时操作日志邮件开关");

        System.out.println("[数据库初始化] 默认配置已就绪");
    }
}
