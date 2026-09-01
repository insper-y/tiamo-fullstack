package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.SysRunLog;
import java.util.List;
import java.util.Map;

/**
 * 系统运行日志 Service 接口
 */
public interface SysRunLogService extends IService<SysRunLog> {

    /**
     * 异步保存运行日志
     */
    void saveLogAsync(SysRunLog log);

    /**
     * 获取各层级调用统计
     */
    List<Map<String, Object>> getLevelStats();

    /**
     * 获取最近24小时每小时调用量
     */
    List<Map<String, Object>> getHourlyStats();

    /**
     * 获取异常最多的方法TOP10
     */
    List<Map<String, Object>> getTopErrorMethods();

    /**
     * 获取耗时最长的方法TOP10
     */
    List<Map<String, Object>> getTopSlowMethods();

    /**
     * 清理指定天数之前的日志
     */
    int cleanOldLogs(int days);
}
