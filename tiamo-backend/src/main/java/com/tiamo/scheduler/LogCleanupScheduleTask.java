package com.tiamo.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.service.SysConfigService;
import com.tiamo.service.SysOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 操作日志定期清理定时任务
 * 每天凌晨2点自动清理超过保留天数的日志
 */
@Component
public class LogCleanupScheduleTask {

    @Autowired
    private SysOperationLogService operationLogService;

    @Autowired
    private SysConfigService configService;

    /**
     * 每天凌晨2点执行日志清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupExpiredLogs() {
        try {
            String enabled = configService.getConfigValue("log.retention.enabled", "true");
            if (!"true".equals(enabled)) {
                System.out.println("[日志清理] 定期清理已关闭，跳过");
                return;
            }
            String daysStr = configService.getConfigValue("log.retention.days", "30");
            int days = Integer.parseInt(daysStr);
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);

            LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.lt(SysOperationLog::getCreateTime, cutoffTime);
            long count = operationLogService.count(wrapper);
            if (count > 0) {
                operationLogService.remove(wrapper);
                System.out.println("[日志清理] 已清理 " + count + " 条超过 " + days + " 天的日志");
            } else {
                System.out.println("[日志清理] 没有需要清理的过期日志");
            }
        } catch (Exception e) {
            System.out.println("[日志清理] 清理失败: " + e.getMessage());
        }
    }
}
