package com.tiamo.config;

import com.tiamo.entity.SysUser;
import com.tiamo.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 数据初始化器
 * 首次启动时自动创建默认管理员账号
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysUserService userService;

    @Value("${app.default-admin.username:admin}")
    private String adminUsername;

    @Value("${app.default-admin.password:Admin123}")
    private String adminPassword;

    @Value("${app.default-admin.nickname:系统管理员}")
    private String adminNickname;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public void run(String... args) {
        // 检查管理员是否已存在
        SysUser existing = userService.getByUsername(adminUsername);
        if (existing != null) {
            System.out.println("[初始化] 管理员账号已存在，跳过创建");
            return;
        }

        // 创建默认管理员
        SysUser admin = new SysUser();
        admin.setUsername(adminUsername);
        admin.setPassword(passwordEncoder.encode(adminPassword));
        admin.setNickname(adminNickname);
        admin.setStatus(1);
        admin.setCreateTime(LocalDateTime.now());
        admin.setUpdateTime(LocalDateTime.now());

        userService.save(admin);
        System.out.println("""
                ==================================================
                  [初始化] 默认管理员账号创建成功!
                  用户名: admin
                  密码:   Admin123
                  请及时修改默认密码!
                ==================================================
                """);
    }
}
