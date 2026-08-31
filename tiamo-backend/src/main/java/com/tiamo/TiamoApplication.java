package com.tiamo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 分销商品数据管理系统 - 启动类
 *
 * 启动后访问:
 *   接口地址: http://localhost:8080/maven/books
 *   前端页面: 将 tiamo-zeng.github.io 的 login.html 中
 *             https://te.zssmh.asia/maven 替换为 http://localhost:8080/maven
 */
@SpringBootApplication
@MapperScan("com.tiamo.mapper")
@EnableScheduling
public class TiamoApplication {

    public static void main(String[] args) {
        SpringApplication.run(TiamoApplication.class, args);
        System.out.println("""
                ==================================================
                  分销商品数据管理系统 启动成功!
                  接口地址: http://localhost:8080/maven/books
                  数据库:   MySQL (tiamo_db)
                ==================================================
                """);
    }
}
