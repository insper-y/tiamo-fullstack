#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
修复表结构：给所有表添加deleted字段，给books表添加deleted_time和deleted_by字段
"""
import pymysql

DB_CONFIG = {
    'host': 'dbconn.sealosbja.site',
    'port': 36273,
    'user': 'root',
    'password': '7b4kzvdr',
    'database': 'mydb',
    'charset': 'utf8mb4',
    'autocommit': True,
}

ALTER_SQLS = [
    # 给sys_user表添加deleted字段
    "ALTER TABLE `sys_user` ADD COLUMN `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记' AFTER `update_time`",
    # 给sys_operation_log表添加deleted字段
    "ALTER TABLE `sys_operation_log` ADD COLUMN `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记' AFTER `create_time`",
    # 给sys_config表添加deleted字段
    "ALTER TABLE `sys_config` ADD COLUMN `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记' AFTER `update_time`",
    # 给recycle_approval表添加deleted字段
    "ALTER TABLE `recycle_approval` ADD COLUMN `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除标记' AFTER `update_time`",
    # 给books表添加deleted_time字段
    "ALTER TABLE `books` ADD COLUMN `deleted_time` DATETIME DEFAULT NULL COMMENT '删除时间' AFTER `deleted`",
    # 给books表添加deleted_by字段
    "ALTER TABLE `books` ADD COLUMN `deleted_by` VARCHAR(100) DEFAULT NULL COMMENT '删除操作人' AFTER `deleted_time`",
]

def main():
    print("=" * 60)
    print("修复表结构")
    print("=" * 60)

    try:
        conn = pymysql.connect(**DB_CONFIG)
        cursor = conn.cursor()

        for sql in ALTER_SQLS:
            try:
                table_name = sql.split('`')[1]
                column_name = sql.split('ADD COLUMN')[1].split('`')[1] if 'ADD COLUMN' in sql else 'N/A'
                print(f"\n执行: {table_name} 表添加 {column_name} 字段...")
                cursor.execute(sql)
                print(f"  ✓ 成功")
            except pymysql.err.OperationalError as e:
                if e.args[0] == 1060:  # Duplicate column name
                    print(f"  - 字段已存在，跳过")
                else:
                    print(f"  ✗ 错误: {e}")
            except Exception as e:
                print(f"  ✗ 错误: {e}")

        # 验证表结构
        print("\n" + "=" * 60)
        print("验证表结构:")
        tables = ['sys_user', 'books', 'sys_operation_log', 'sys_config', 'recycle_approval']
        for table in tables:
            cursor.execute(f"DESCRIBE `{table}`")
            columns = cursor.fetchall()
            col_names = [col[0] for col in columns]
            has_deleted = 'deleted' in col_names
            print(f"\n  {table} ({len(columns)}个字段):")
            print(f"    字段: {', '.join(col_names)}")
            print(f"    有deleted字段: {'✓' if has_deleted else '✗'}")

        # 验证数据量
        print("\n" + "=" * 60)
        print("数据量统计:")
        cursor.execute("SELECT COUNT(*) FROM books")
        print(f"  books: {cursor.fetchone()[0]:,} 条")
        cursor.execute("SELECT COUNT(*) FROM sys_user")
        print(f"  sys_user: {cursor.fetchone()[0]} 条")

        cursor.close()
        conn.close()
        print("\n✓ 表结构修复完成！")

    except Exception as e:
        print(f"\n✗ 错误: {e}")
        import traceback
        traceback.print_exc()

if __name__ == '__main__':
    main()
