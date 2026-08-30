-- ============================================================
-- Tiamo AI 数据管理系统 - 数据库初始化脚本
-- 数据库: MySQL 8.0+
-- ============================================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS tiamo_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE tiamo_db;

-- ------------------------------------------------------------
-- 系统用户表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `username`    VARCHAR(100) NOT NULL COMMENT '用户名',
    `password`    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
    `nickname`    VARCHAR(100) DEFAULT NULL COMMENT '昵称',
    `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
    `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_phone` (`phone`),
    KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ------------------------------------------------------------
-- 分销商品数据表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `books`;
CREATE TABLE `books` (
    `id`          INT          NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `name`        VARCHAR(255) DEFAULT NULL COMMENT '分销软件',
    `type`        VARCHAR(255) DEFAULT NULL COMMENT '微信账号',
    `description` TEXT         DEFAULT NULL COMMENT '软件账号',
    `aa`          VARCHAR(255) DEFAULT NULL COMMENT '微信备注名',
    `bd`          VARCHAR(255) DEFAULT NULL COMMENT '商品ID',
    `ac`          VARCHAR(500) DEFAULT NULL COMMENT '商品链接',
    `ab`          VARCHAR(500) DEFAULT NULL COMMENT '商品主图',
    `ax`          VARCHAR(500) DEFAULT NULL COMMENT '商品标题',
    `user_id`     BIGINT       DEFAULT NULL COMMENT '所属用户ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除标记 0-未删除 1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_name` (`name`),
    KEY `idx_type` (`type`),
    KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分销商品数据表';

-- 测试数据
INSERT INTO `books` (`name`, `type`, `description`, `aa`, `bd`, `ac`, `ab`, `ax`) VALUES
('分销助手A', 'wx_account_001', 'soft_acc_001', '客户张三', '10001', 'https://example.com/product/10001', 'https://example.com/img/10001.jpg', '示例商品标题1'),
('分销助手B', 'wx_account_002', 'soft_acc_002', '客户李四', '10002', 'https://example.com/product/10002', 'https://example.com/img/10002.jpg', '示例商品标题2');

-- ============================================================
-- 说明：默认管理员账号由后端 DataInitializer 自动创建
-- 用户名: admin  密码: Admin123
-- 首次启动时自动写入数据库，无需手动插入
-- ============================================================

-- ------------------------------------------------------------
-- 操作日志表
-- ------------------------------------------------------------
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`         BIGINT       DEFAULT NULL COMMENT '操作人ID',
    `username`        VARCHAR(100) DEFAULT NULL COMMENT '操作人用户名',
    `module`          VARCHAR(100) DEFAULT NULL COMMENT '操作模块',
    `description`     VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
    `method`          VARCHAR(255) DEFAULT NULL COMMENT '请求方法',
    `params`          TEXT         DEFAULT NULL COMMENT '请求参数',
    `ip`              VARCHAR(50)  DEFAULT NULL COMMENT '请求IP',
    `url`             VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
    `operation_type`  VARCHAR(20)  DEFAULT 'OTHER' COMMENT '操作类型: LOGIN/LOGOUT/CREATE/UPDATE/DELETE/QUERY/OTHER',
    `cost_time`       BIGINT       DEFAULT NULL COMMENT '执行耗时(毫秒)',
    `status`          VARCHAR(10)  DEFAULT 'SUCCESS' COMMENT '操作结果: SUCCESS/FAIL',
    `error_msg`       TEXT         DEFAULT NULL COMMENT '错误信息',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_username` (`username`),
    KEY `idx_module` (`module`),
    KEY `idx_operation_type` (`operation_type`),
    KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
