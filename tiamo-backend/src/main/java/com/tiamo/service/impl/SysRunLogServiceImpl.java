package com.tiamo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.SysRunLog;
import com.tiamo.mapper.SysRunLogMapper;
import com.tiamo.service.SysRunLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * 系统运行日志 Service 实现类
 */
@Service
public class SysRunLogServiceImpl extends ServiceImpl<SysRunLogMapper, SysRunLog> implements SysRunLogService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @Async("logExecutor")
    public void saveLogAsync(SysRunLog log) {
        try {
            save(log);
        } catch (Exception e) {
            // 日志保存失败不影响业务
            System.err.println("运行日志保存失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getLevelStats() {
        return baseMapper.getLevelStats();
    }

    @Override
    public List<Map<String, Object>> getHourlyStats() {
        return baseMapper.getHourlyStats();
    }

    @Override
    public List<Map<String, Object>> getTopErrorMethods() {
        return baseMapper.getTopErrorMethods();
    }

    @Override
    public List<Map<String, Object>> getTopSlowMethods() {
        return baseMapper.getTopSlowMethods();
    }

    @Override
    public int cleanOldLogs(int days) {
        return baseMapper.cleanOldLogs(days);
    }

    @Override
    public int cleanTodayLogs() {
        return baseMapper.cleanTodayLogs();
    }

    @Override
    public Map<String, Object> getAggregateStats() {
        String sql = "SELECT " +
                "COUNT(*) AS totalCount, " +
                "SUM(CASE WHEN DATE(create_time) = CURDATE() THEN 1 ELSE 0 END) AS todayCount, " +
                "SUM(CASE WHEN status = 'FAIL' THEN 1 ELSE 0 END) AS errorCount, " +
                "SUM(CASE WHEN status = 'FAIL' AND DATE(create_time) = CURDATE() THEN 1 ELSE 0 END) AS todayErrorCount " +
                "FROM sys_run_log";
        try {
            Map<String, Object> result = jdbcTemplate.queryForMap(sql);
            Map<String, Object> converted = new java.util.HashMap<>();
            for (Map.Entry<String, Object> entry : result.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof Number) {
                    converted.put(entry.getKey(), ((Number) value).longValue());
                } else {
                    converted.put(entry.getKey(), value);
                }
            }
            return converted;
        } catch (Exception e) {
            Map<String, Object> fallback = new java.util.HashMap<>();
            fallback.put("totalCount", 0L);
            fallback.put("todayCount", 0L);
            fallback.put("errorCount", 0L);
            fallback.put("todayErrorCount", 0L);
            return fallback;
        }
    }
}
