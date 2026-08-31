package com.tiamo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.SysOperationLogService;
import com.tiamo.service.SysConfigService;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 Controller
 * 提供操作日志的查询、批量删除、定期清理等接口
 */
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    @Autowired
    private SysOperationLogService operationLogService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserServiceImpl userService;

    @Autowired
    private SysConfigService configService;

    /**
     * 分页查询操作日志
     */
    @GetMapping
    @OperationLog(module = "日志管理", description = "查询操作日志", operationType = "QUERY")
    public Result<Page<SysOperationLog>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        if (module != null && !module.isEmpty()) {
            wrapper.like(SysOperationLog::getModule, module);
        }
        if (operationType != null && !operationType.isEmpty()) {
            wrapper.eq(SysOperationLog::getOperationType, operationType);
        }
        if (username != null && !username.isEmpty()) {
            wrapper.like(SysOperationLog::getUsername, username);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysOperationLog::getStatus, status);
        }
        if (startTime != null && !startTime.isEmpty()) {
            wrapper.ge(SysOperationLog::getCreateTime, startTime);
        }
        if (endTime != null && !endTime.isEmpty()) {
            wrapper.le(SysOperationLog::getCreateTime, endTime);
        }
        wrapper.orderByDesc(SysOperationLog::getCreateTime);
        Page<SysOperationLog> pageResult = operationLogService.page(new Page<>(page, size), wrapper);
        return new Result<>(200, pageResult, "查询成功");
    }

    /**
     * 查询最近的操作日志
     */
    @GetMapping("/recent")
    public Result<List<SysOperationLog>> getRecentLogs(@RequestParam(defaultValue = "10") Integer limit) {
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysOperationLog::getCreateTime).last("LIMIT " + limit);
        return new Result<>(200, operationLogService.list(wrapper), "查询成功");
    }

    /**
     * 根据ID查询日志详情
     */
    @GetMapping("/{id}")
    public Result<SysOperationLog> getLogById(@PathVariable Long id) {
        SysOperationLog log = operationLogService.getById(id);
        if (log == null) return new Result<>(404, null, "日志不存在");
        return new Result<>(200, log, "查询成功");
    }

    /**
     * 获取操作统计
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats() {
        Map<String, Object> stats = new java.util.HashMap<>();
        long total = operationLogService.count();
        long successCount = operationLogService.count(new LambdaQueryWrapper<SysOperationLog>().eq(SysOperationLog::getStatus, "SUCCESS"));
        long failCount = total - successCount;
        stats.put("total", total);
        stats.put("success", successCount);
        stats.put("fail", failCount);
        stats.put("successRate", total > 0 ? String.format("%.1f%%", (successCount * 100.0 / total)) : "0%");
        return new Result<>(200, stats, "查询成功");
    }

    /**
     * 批量删除操作日志（管理员）
     * POST /api/logs/batch-delete
     */
    @PostMapping("/batch-delete")
    @OperationLog(module = "日志管理", description = "批量删除操作日志", operationType = "DELETE")
    public Result<String> batchDelete(
            @RequestBody Map<String, List<Long>> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作，仅管理员可删除日志");
        }
        List<Long> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return new Result<>(400, null, "请选择要删除的日志");
        }
        boolean flag = operationLogService.removeByIds(ids);
        if (flag) {
            return new Result<>(200, null, "批量删除成功（共" + ids.size() + "条）");
        }
        return new Result<>(500, null, "批量删除失败");
    }

    /**
     * 按条件清理操作日志（管理员）
     * POST /api/logs/clean
     * body: {"days": 30, "module": "日志管理", "status": "SUCCESS"}
     */
    @PostMapping("/clean")
    @OperationLog(module = "日志管理", description = "清理操作日志", operationType = "DELETE")
    public Result<String> cleanLogs(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作，仅管理员可清理日志");
        }
        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        // 按天数清理
        if (request.get("days") != null) {
            int days = Integer.parseInt(request.get("days").toString());
            wrapper.lt(SysOperationLog::getCreateTime, LocalDateTime.now().minusDays(days));
        }
        if (request.get("module") != null && !request.get("module").toString().isEmpty()) {
            wrapper.eq(SysOperationLog::getModule, request.get("module").toString());
        }
        if (request.get("status") != null && !request.get("status").toString().isEmpty()) {
            wrapper.eq(SysOperationLog::getStatus, request.get("status").toString());
        }
        if (request.get("operationType") != null && !request.get("operationType").toString().isEmpty()) {
            wrapper.eq(SysOperationLog::getOperationType, request.get("operationType").toString());
        }
        long count = operationLogService.count(wrapper);
        boolean flag = operationLogService.remove(wrapper);
        if (flag) {
            return new Result<>(200, null, "清理成功，共删除" + count + "条日志");
        }
        return new Result<>(500, null, "清理失败");
    }

    /**
     * 获取日志保留天数配置（管理员）
     * GET /api/logs/retention-days
     */
    @GetMapping("/retention-days")
    public Result<Map<String, Object>> getRetentionDays(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问");
        }
        String days = configService.getConfigValue("log.retention.days", "30");
        String enabled = configService.getConfigValue("log.retention.enabled", "true");
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("days", Integer.parseInt(days));
        data.put("enabled", "true".equals(enabled));
        return new Result<>(200, data, "查询成功");
    }

    /**
     * 设置日志保留天数配置（管理员）
     * PUT /api/logs/retention-days
     */
    @PutMapping("/retention-days")
    @OperationLog(module = "日志管理", description = "设置日志保留策略", operationType = "UPDATE")
    public Result<String> setRetentionDays(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作");
        }
        if (request.get("days") != null) {
            configService.setConfigValue("log.retention.days", request.get("days").toString(), "操作日志保留天数");
        }
        if (request.get("enabled") != null) {
            configService.setConfigValue("log.retention.enabled", request.get("enabled").toString(), "操作日志定期清理开关");
        }
        return new Result<>(200, null, "日志保留策略已更新");
    }

    /* ==================== 辅助方法 ==================== */

    private boolean isAdmin(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token == null || !jwtUtil.validateToken(token)) return false;
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) return false;
            SysUser user = userService.getById(userId);
            return user != null && user.getRole() != null && user.getRole() == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
