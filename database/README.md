# Tiamo AI 系统数据库

## 数据库信息
- 类型: MySQL 8.0
- 字符集: utf8mb4
- 导出时间: 2026-09-01

## 表结构
| 表名 | 说明 | 数据量 |
|------|------|--------|
| sys_user | 用户表（管理员/普通用户） | 3条 |
| books | 商品数据表 | 1002条 |
| sys_operation_log | 操作日志表 | - |
| sys_config | 系统配置表 | - |
| recycle_approval | 回收站审批表 | - |

## 导入方法

### 方法1: 命令行导入
```bash
mysql -h <host> -P <port> -u <username> -p <database> < tiamo_db_export.sql
```

### 方法2: Navicat / DBeaver 等工具
1. 新建数据库，字符集选择 utf8mb4
2. 右键数据库 -> 运行SQL文件
3. 选择 tiamo_db_export.sql
4. 点击开始

## 默认账号
- 管理员: admin / Admin123
- 普通用户: 2149212156 / (自行设置)

## 注意事项
- 所有表都有 `deleted` 字段（逻辑删除），0=未删除，1=已删除
- books 表有 `deleted_time` 和 `deleted_by` 字段记录删除信息
- 导入前请确保数据库字符集为 utf8mb4
