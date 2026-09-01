package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.SysRunLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

/**
 * 系统运行日志 Mapper
 */
@Mapper
public interface SysRunLogMapper extends BaseMapper<SysRunLog> {

    /**
     * 统计各层级调用数量
     */
    @Select("SELECT level, COUNT(*) as count, AVG(cost_time) as avg_cost, MAX(cost_time) as max_cost FROM sys_run_log WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY) GROUP BY level")
    List<Map<String, Object>> getLevelStats();

    /**
     * 统计最近24小时每小时调用量
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d %H:00') as hour, COUNT(*) as count FROM sys_run_log WHERE create_time >= DATE_SUB(NOW(), INTERVAL 24 HOUR) GROUP BY hour ORDER BY hour")
    List<Map<String, Object>> getHourlyStats();

    /**
     * 统计异常最多的方法TOP10
     */
    @Select("SELECT full_method, COUNT(*) as error_count FROM sys_run_log WHERE status = 'FAIL' AND create_time >= DATE_SUB(NOW(), INTERVAL 7 DAY) GROUP BY full_method ORDER BY error_count DESC LIMIT 10")
    List<Map<String, Object>> getTopErrorMethods();

    /**
     * 统计耗时最长的方法TOP10
     */
    @Select("SELECT full_method, AVG(cost_time) as avg_cost, MAX(cost_time) as max_cost, COUNT(*) as call_count FROM sys_run_log WHERE create_time >= DATE_SUB(NOW(), INTERVAL 1 DAY) GROUP BY full_method ORDER BY avg_cost DESC LIMIT 10")
    List<Map<String, Object>> getTopSlowMethods();

    /**
     * 清理指定天数之前的日志
     */
    @Delete("DELETE FROM sys_run_log WHERE TIMESTAMPDIFF(DAY, create_time, NOW()) >= #{days}")
    int cleanOldLogs(@Param("days") int days);
}
