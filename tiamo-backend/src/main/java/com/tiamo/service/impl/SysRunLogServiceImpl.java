package com.tiamo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.SysRunLog;
import com.tiamo.mapper.SysRunLogMapper;
import com.tiamo.service.SysRunLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;

/**
 * 系统运行日志 Service 实现类
 */
@Service
public class SysRunLogServiceImpl extends ServiceImpl<SysRunLogMapper, SysRunLog> implements SysRunLogService {

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
}
