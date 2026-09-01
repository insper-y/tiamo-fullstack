#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
删除旧books表，用正确schema重建所有表，注入1千条数据
"""
import pymysql
import time
import random
import string

DB_CONFIG = {
    'host': 'dbconn.sealosbja.site',
    'port': 36273,
    'user': 'root',
    'password': '7b4kzvdr',
    'database': 'mydb',
    'charset': 'utf8mb4',
    'autocommit': False,
}

# 正确的建表语句
CREATE_TABLES = [
    # 系统用户表
    """CREATE TABLE IF NOT EXISTS `sys_user` (
        `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
        `username`    VARCHAR(100) NOT NULL COMMENT '用户名',
        `password`    VARCHAR(255) NOT NULL COMMENT '密码（BCrypt加密）',
        `nickname`    VARCHAR(100) DEFAULT NULL COMMENT '昵称',
        `email`       VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
        `phone`       VARCHAR(20)  DEFAULT NULL COMMENT '手机号',
        `status`      TINYINT      DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
        `role`        TINYINT      DEFAULT 0 COMMENT '角色 0-普通用户 1-管理员',
        `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_username` (`username`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表'""",

    # 分销商品数据表（正确结构）
    """CREATE TABLE IF NOT EXISTS `books` (
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
        KEY `idx_user_id` (`user_id`),
        KEY `idx_deleted` (`deleted`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分销商品数据表'""",

    # 操作日志表
    """CREATE TABLE IF NOT EXISTS `sys_operation_log` (
        `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
        `user_id`         BIGINT       DEFAULT NULL COMMENT '操作人ID',
        `username`        VARCHAR(100) DEFAULT NULL COMMENT '操作人用户名',
        `module`          VARCHAR(100) DEFAULT NULL COMMENT '操作模块',
        `description`     VARCHAR(500) DEFAULT NULL COMMENT '操作描述',
        `method`          VARCHAR(255) DEFAULT NULL COMMENT '请求方法',
        `params`          TEXT         DEFAULT NULL COMMENT '请求参数',
        `ip`              VARCHAR(50)  DEFAULT NULL COMMENT '请求IP',
        `url`             VARCHAR(500) DEFAULT NULL COMMENT '请求URL',
        `operation_type`  VARCHAR(20)  DEFAULT 'OTHER' COMMENT '操作类型',
        `cost_time`       BIGINT       DEFAULT NULL COMMENT '执行耗时(毫秒)',
        `status`          VARCHAR(10)  DEFAULT 'SUCCESS' COMMENT '操作结果',
        `error_msg`       TEXT         DEFAULT NULL COMMENT '错误信息',
        `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
        PRIMARY KEY (`id`),
        KEY `idx_user_id` (`user_id`),
        KEY `idx_create_time` (`create_time`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表'""",

    # 系统配置表
    """CREATE TABLE IF NOT EXISTS `sys_config` (
        `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
        `config_key`  VARCHAR(100) NOT NULL COMMENT '配置键',
        `config_value` TEXT        DEFAULT NULL COMMENT '配置值',
        `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
        `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
        `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        PRIMARY KEY (`id`),
        UNIQUE KEY `uk_config_key` (`config_key`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表'""",

    # 回收站审批表
    """CREATE TABLE IF NOT EXISTS `recycle_approval` (
        `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
        `book_id`         INT          NOT NULL COMMENT '商品ID',
        `book_title`      VARCHAR(500) DEFAULT NULL COMMENT '商品标题',
        `applicant_id`    BIGINT       DEFAULT NULL COMMENT '申请人ID',
        `applicant_name`  VARCHAR(100) DEFAULT NULL COMMENT '申请人用户名',
        `approval_type`   VARCHAR(20)  NOT NULL COMMENT '审批类型: RESTORE/DELETE',
        `status`          VARCHAR(20)  DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED',
        `reject_reason`   VARCHAR(500) DEFAULT NULL COMMENT '拒绝原因',
        `approver_id`     BIGINT       DEFAULT NULL COMMENT '审批人ID',
        `approver_name`   VARCHAR(100) DEFAULT NULL COMMENT '审批人用户名',
        `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
        `update_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
        PRIMARY KEY (`id`),
        KEY `idx_book_id` (`book_id`),
        KEY `idx_applicant_id` (`applicant_id`),
        KEY `idx_status` (`status`)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='回收站审批表'""",
]

# 随机数据池
SOFTWARE_NAMES = ['好省', '粉象生活', '高佣联盟', '花生日记', '蜜源', '美逛', '淘小铺', '东小店', '芬香', '悦拜']
PRODUCT_TITLES = [
    '夏季新款女装连衣裙', '男士休闲运动鞋', '儿童益智玩具积木', '家用智能扫地机器人',
    '无线蓝牙耳机降噪', '不锈钢保温杯大容量', '纯棉T恤男女同款', '笔记本电脑轻薄本',
    '手机壳防摔保护套', '充电宝20000毫安', '厨房用品刀具套装', '床上用品四件套',
    '护肤品套装补水保湿', '零食大礼包网红小吃', '水果新鲜当季整箱', '茶叶礼盒装高档',
]

def random_string(length=8):
    return ''.join(random.choices(string.ascii_lowercase + string.digits, k=length))

def main():
    print("=" * 60)
    print("重建数据库表结构并注入数据")
    print("=" * 60)

    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()

        # 1. 删除旧的books表（结构不正确）
        print("\n[1/4] 删除旧的books表...")
        cursor.execute("DROP TABLE IF EXISTS `books`")
        conn.commit()
        print("  ✓ 旧books表已删除")

        # 2. 创建所有表
        print("\n[2/4] 创建所有表...")
        for i, sql in enumerate(CREATE_TABLES):
            table_name = sql.split('`')[1]
            cursor.execute(sql)
            print(f"  ✓ {table_name} 表已创建")
        conn.commit()

        # 3. 插入默认管理员账号
        print("\n[3/4] 插入默认管理员账号...")
        # BCrypt加密的 Admin123
        admin_password = "$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi"
        cursor.execute("""
            INSERT IGNORE INTO sys_user (username, password, nickname, role, status)
            VALUES ('admin', %s, '系统管理员', 1, 1)
        """, (admin_password,))
        conn.commit()
        print("  ✓ 默认管理员 admin/Admin123 已创建")

        # 4. 注入1千条商品数据
        print("\n[4/4] 注入1千条商品数据...")
        insert_sql = """
            INSERT INTO books (name, type, description, aa, bd, ac, ab, ax, user_id, deleted)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """

        batch_data = []
        for i in range(1000):
            batch_data.append((
                random.choice(SOFTWARE_NAMES),
                f'wx_{i:08d}',
                f'user_{i:010d}',
                f'客户{i:05d}',
                f'PRD{i:010d}',
                f'https://item.taobao.com/item.htm?id={i}',
                f'https://img.alicdn.com/imgextra/i{i % 10}/O1CN01{random_string(10)}.jpg',
                random.choice(PRODUCT_TITLES) + f'_{i}',
                1,  # user_id = admin
                0,  # deleted = 0
            ))

        cursor.executemany(insert_sql, batch_data)
        conn.commit()
        print(f"  ✓ 已注入 {len(batch_data)} 条商品数据")

        # 验证
        print("\n" + "=" * 60)
        print("验证结果:")
        cursor.execute("SELECT COUNT(*) FROM books")
        books_count = cursor.fetchone()[0]
        cursor.execute("SELECT COUNT(*) FROM sys_user")
        user_count = cursor.fetchone()[0]
        print(f"  books表记录数: {books_count:,}")
        print(f"  sys_user表记录数: {user_count}")

        # 查看表结构
        cursor.execute("DESCRIBE books")
        columns = cursor.fetchall()
        print(f"\n  books表字段 ({len(columns)}个):")
        for col in columns:
            print(f"    - {col[0]} ({col[1]})")

        cursor.close()
        conn.close()
        print("\n✓ 全部完成！")

    except Exception as e:
        print(f"\n✗ 错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()
