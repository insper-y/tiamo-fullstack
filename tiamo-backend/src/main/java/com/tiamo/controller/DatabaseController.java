package com.tiamo.controller;

import com.tiamo.common.Result;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据库管理 Controller
 * 提供数据库表管理、SQL查询、备份等功能，仅管理员可访问
 * 所有接口路径: /api/db/**
 */
@RestController
@RequestMapping("/api/db")
public class DatabaseController {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SysUserServiceImpl userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取所有表列表
     * GET /api/db/tables
     */
    @GetMapping("/tables")
    public Result<List<Map<String, Object>>> getTables(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问，仅管理员可操作");
        }
        try {
            // 查询所有表
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME as tableName, TABLE_ROWS as rowCount, " +
                "ROUND(DATA_LENGTH/1024/1024, 2) as dataSizeMB, " +
                "ROUND(INDEX_LENGTH/1024/1024, 2) as indexSizeMB, " +
                "TABLE_COMMENT as comment, CREATE_TIME as createTime, UPDATE_TIME as updateTime " +
                "FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME"
            );
            return new Result<>(200, tables, "查询成功");
        } catch (Exception e) {
            return new Result<>(500, null, "查询失败，请稍后重试");
        }
    }

    /**
     * 获取表结构
     * GET /api/db/tables/{tableName}/structure
     */
    @GetMapping("/tables/{tableName}/structure")
    public Result<Map<String, Object>> getTableStructure(
            @PathVariable String tableName,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问，仅管理员可操作");
        }
        try {
            // 防止SQL注入，只允许字母数字下划线
            if (!tableName.matches("^[a-zA-Z0-9_]+$")) {
                return new Result<>(400, null, "表名格式不正确");
            }
            // 查询字段信息
            List<Map<String, Object>> columns = jdbcTemplate.queryForList(
                "SELECT COLUMN_NAME as columnName, COLUMN_TYPE as columnType, IS_NULLABLE as nullable, " +
                "COLUMN_KEY as columnKey, COLUMN_DEFAULT as defaultValue, EXTRA as extra, " +
                "COLUMN_COMMENT as comment, ORDINAL_POSITION as position " +
                "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY ORDINAL_POSITION",
                tableName
            );
            // 查询索引信息
            List<Map<String, Object>> indexes = jdbcTemplate.queryForList(
                "SELECT INDEX_NAME as indexName, COLUMN_NAME as columnName, NON_UNIQUE as nonUnique, " +
                "SEQ_IN_INDEX as seqInIndex, INDEX_TYPE as indexType " +
                "FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? ORDER BY INDEX_NAME, SEQ_IN_INDEX",
                tableName
            );
            Map<String, Object> result = new HashMap<>();
            result.put("columns", columns);
            result.put("indexes", indexes);
            return new Result<>(200, result, "查询成功");
        } catch (Exception e) {
            return new Result<>(500, null, "查询失败，请稍后重试");
        }
    }

    /**
     * 执行SQL查询（仅允许SELECT）
     * POST /api/db/query
     * body: {"sql": "SELECT * FROM users LIMIT 10"}
     */
    @PostMapping("/query")
    public Result<Map<String, Object>> executeQuery(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问，仅管理员可操作");
        }
        String sql = request.get("sql");
        if (sql == null || sql.trim().isEmpty()) {
            return new Result<>(400, null, "SQL语句不能为空");
        }
        sql = sql.trim();
        // 仅允许SELECT查询
        if (!sql.toUpperCase().startsWith("SELECT") && !sql.toUpperCase().startsWith("SHOW") && !sql.toUpperCase().startsWith("DESC")) {
            return new Result<>(400, null, "仅允许执行SELECT、SHOW、DESC查询语句");
        }
        // 限制最多返回1000条
        if (!sql.toUpperCase().contains("LIMIT")) {
            sql = sql + " LIMIT 1000";
        }
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            Map<String, Object> data = new HashMap<>();
            data.put("results", results);
            data.put("total", results.size());
            data.put("columns", results.isEmpty() ? new ArrayList<>() : new ArrayList<>(results.get(0).keySet()));
            return new Result<>(200, data, "查询成功，返回" + results.size() + "条记录");
        } catch (Exception e) {
            return new Result<>(500, null, "查询失败，请稍后重试");
        }
    }

    /**
     * 数据库备份（导出所有表结构和数据）
     * GET /api/db/backup
     */
    @GetMapping("/backup")
    public Result<String> backupDatabase(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问，仅管理员可操作");
        }
        try {
            StringBuilder sql = new StringBuilder();
            sql.append("-- Tiamo AI 数据库备份\n");
            sql.append("-- 备份时间: ").append(new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).append("\n\n");
            sql.append("SET NAMES utf8mb4;\n");
            sql.append("SET FOREIGN_KEY_CHECKS = 0;\n\n");

            // 获取所有表
            List<Map<String, Object>> tables = jdbcTemplate.queryForList(
                "SELECT TABLE_NAME as tableName FROM information_schema.TABLES WHERE TABLE_SCHEMA = DATABASE() ORDER BY TABLE_NAME"
            );

            for (Map<String, Object> table : tables) {
                String tableName = (String) table.get("tableName");
                sql.append("-- ----------------------------\n");
                sql.append("-- 表结构: ").append(tableName).append("\n");
                sql.append("-- ----------------------------\n");
                sql.append("DROP TABLE IF EXISTS `").append(tableName).append("`;\n");

                // 获取建表语句
                List<Map<String, Object>> createTable = jdbcTemplate.queryForList("SHOW CREATE TABLE `" + tableName + "`");
                if (!createTable.isEmpty()) {
                    String createSql = (String) createTable.get(0).get("Create Table");
                    sql.append(createSql).append(";\n\n");
                }

                // 获取表数据
                sql.append("-- ----------------------------\n");
                sql.append("-- 表数据: ").append(tableName).append("\n");
                sql.append("-- ----------------------------\n");
                List<Map<String, Object>> data = jdbcTemplate.queryForList("SELECT * FROM `" + tableName + "`");
                for (Map<String, Object> row : data) {
                    StringBuilder insert = new StringBuilder("INSERT INTO `").append(tableName).append("` VALUES (");
                    boolean first = true;
                    for (Object value : row.values()) {
                        if (!first) insert.append(", ");
                        if (value == null) {
                            insert.append("NULL");
                        } else if (value instanceof Number) {
                            insert.append(value);
                        } else {
                            insert.append("'").append(value.toString().replace("'", "''")).append("'");
                        }
                        first = false;
                    }
                    insert.append(");\n");
                    sql.append(insert);
                }
                sql.append("\n");
            }

            sql.append("SET FOREIGN_KEY_CHECKS = 1;\n");

            // 返回SQL内容
            return new Result<>(200, sql.toString(), "备份成功，共" + tables.size() + "个表");
        } catch (Exception e) {
            return new Result<>(500, null, "备份失败，请稍后重试");
        }
    }

    /**
     * 验证是否为管理员
     */
    private boolean isAdmin(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token == null || !jwtUtil.validateToken(token)) {
                return false;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) return false;
            SysUser user = userService.getById(userId);
            return user != null && user.getRole() != null && user.getRole() == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
