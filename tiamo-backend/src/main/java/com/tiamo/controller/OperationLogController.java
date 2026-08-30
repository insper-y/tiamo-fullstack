package com.tiamo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.service.SysOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 操作日志 Controller
 * 提供操作日志的查询接口（需认证）
 */
@RestController
@RequestMapping("/api/logs")
public class OperationLogController {

    @Autowired
    private SysOperationLogService operationLogService;

    /**
     * 分页查询操作日志
     * GET /api/logs?page=1&size=20&module=用户管理&operationType=LOGIN
     */
    @GetMapping
    @OperationLog(module = "日志管理", description = "查询操作日志", operationType = "QUERY")
    public Result<Page<SysOperationLog>> getLogs(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String status) {

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

        wrapper.orderByDesc(SysOperationLog::getCreateTime);

        Page<SysOperationLog> pageResult = operationLogService.page(
                new Page<>(page, size), wrapper);

        return new Result<>(200, pageResult, "查询成功");
    }

    /**
     * 查询最近的操作日志（不分页）
     * GET /api/logs/recent?limit=10
     */
    @GetMapping("/recent")
    public Result<List<SysOperationLog>> getRecentLogs(
            @RequestParam(defaultValue = "10") Integer limit) {

        LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysOperationLog::getCreateTime)
                .last("LIMIT " + limit);

        List<SysOperationLog> list = operationLogService.list(wrapper);
        return new Result<>(200, list, "查询成功");
    }

    /**
     * 根据ID查询日志详情
     * GET /api/logs/{id}
     */
    @GetMapping("/{id}")
    public Result<SysOperationLog> getLogById(@PathVariable Long id) {
        SysOperationLog log = operationLogService.getById(id);
        if (log == null) {
            return new Result<>(404, null, "日志不存在");
        }
        return new Result<>(200, log, "查询成功");
    }

    /**
     * 获取操作统计
     * GET /api/logs/stats
     */
    @GetMapping("/stats")
    public Result<java.util.Map<String, Object>> getStats() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();

        long total = operationLogService.count();
        long successCount = operationLogService.count(
                new LambdaQueryWrapper<SysOperationLog>().eq(SysOperationLog::getStatus, "SUCCESS"));
        long failCount = total - successCount;

        stats.put("total", total);
        stats.put("success", successCount);
        stats.put("fail", failCount);
        stats.put("successRate", total > 0 ?
                String.format("%.1f%%", (successCount * 100.0 / total)) : "0%");

        return new Result<>(200, stats, "查询成功");
    }
}
