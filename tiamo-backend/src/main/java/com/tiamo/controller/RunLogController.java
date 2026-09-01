package com.tiamo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tiamo.common.Result;
import com.tiamo.entity.SysRunLog;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysRunLogServiceImpl;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 系统运行日志 Controller
 * 提供运行日志的查询、统计、清理等接口
 * 所有接口路径: /api/run-log/**
 */
@RestController
@RequestMapping("/api/run-log")
public class RunLogController {

    @Autowired
    private SysRunLogServiceImpl runLogService;

    @Autowired
    private SysUserServiceImpl userService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 分页查询运行日志（仅管理员）
     * GET /api/run-log/list?page=1&size=20&level=CONTROLLER&status=SUCCESS&keyword=xxx
     */
    @GetMapping("/list")
    public Result<Map<String, Object>> getRunLogList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 验证管理员权限
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问，仅管理员可查看运行日志");
        }

        LambdaQueryWrapper<SysRunLog> wrapper = new LambdaQueryWrapper<>();
        if (level != null && !level.isEmpty()) {
            wrapper.eq(SysRunLog::getLevel, level);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(SysRunLog::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(SysRunLog::getClassName, keyword)
                    .or().like(SysRunLog::getMethodName, keyword)
                    .or().like(SysRunLog::getFullMethod, keyword)
                    .or().like(SysRunLog::getUsername, keyword)
                    .or().like(SysRunLog::getException, keyword));
        }
        wrapper.orderByDesc(SysRunLog::getId);

        Page<SysRunLog> pageResult = runLogService.page(new Page<>(page, size), wrapper);

        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("pages", pageResult.getPages());
        data.put("current", pageResult.getCurrent());
        data.put("size", pageResult.getSize());

        return new Result<>(200, data, "查询成功");
    }

    /**
     * 获取运行日志详情（仅管理员）
     * GET /api/run-log/{id}
     */
    @GetMapping("/{id}")
    public Result<SysRunLog> getRunLogDetail(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问");
        }
        SysRunLog log = runLogService.getById(id);
        if (log == null) {
            return new Result<>(404, null, "日志不存在");
        }
        return new Result<>(200, log, "查询成功");
    }

    /**
     * 获取系统运行状态统计（仅管理员）
     * GET /api/run-log/stats
     */
    @GetMapping("/stats")
    public Result<Map<String, Object>> getRunLogStats(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限访问");
        }

        Map<String, Object> stats = new HashMap<>();

        // 各层级调用统计
        stats.put("levelStats", runLogService.getLevelStats());

        // 最近24小时每小时调用量
        stats.put("hourlyStats", runLogService.getHourlyStats());

        // 异常最多的方法TOP10
        stats.put("topErrorMethods", runLogService.getTopErrorMethods());

        // 耗时最长的方法TOP10
        stats.put("topSlowMethods", runLogService.getTopSlowMethods());

        // 总调用量
        long totalCount = runLogService.count();
        stats.put("totalCount", totalCount);

        // 今日调用量
        long todayCount = runLogService.count(new LambdaQueryWrapper<SysRunLog>()
                .apply("DATE(create_time) = CURDATE()"));
        stats.put("todayCount", todayCount);

        // 异常总数
        long errorCount = runLogService.count(new LambdaQueryWrapper<SysRunLog>()
                .eq(SysRunLog::getStatus, "FAIL"));
        stats.put("errorCount", errorCount);

        // 今日异常数
        long todayErrorCount = runLogService.count(new LambdaQueryWrapper<SysRunLog>()
                .eq(SysRunLog::getStatus, "FAIL")
                .apply("DATE(create_time) = CURDATE()"));
        stats.put("todayErrorCount", todayErrorCount);

        return new Result<>(200, stats, "查询成功");
    }

    /**
     * 清理指定天数之前的运行日志（仅管理员）
     * DELETE /api/run-log/clean?days=30
     */
    @DeleteMapping("/clean")
    public Result<String> cleanOldLogs(
            @RequestParam(defaultValue = "30") int days,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作");
        }
        int count = runLogService.cleanOldLogs(days);
        return new Result<>(200, null, "清理成功，共删除" + count + "条日志");
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
