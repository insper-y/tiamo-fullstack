package com.tiamo.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.service.EmailService;
import com.tiamo.service.SysOperationLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 操作日志定时汇总任务
 * 每天晚上20:00自动发送操作日志汇总邮件
 * 统计时间段：前天20:00 至 当天20:00
 */
@Component
public class OperationLogScheduleTask {

    @Autowired
    private SysOperationLogService operationLogService;

    @Autowired
    private EmailService emailService;

    @Value("${app.log-email.to:}")
    private String logEmailTo;

    @Value("${app.log-email.enabled:false}")
    private boolean logEmailEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每天晚上20:00执行
     * cron: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void sendDailyOperationLogReport() {
        if (!logEmailEnabled || logEmailTo == null || logEmailTo.isEmpty()) {
            System.out.println("[定时任务] 操作日志邮件推送未开启，跳过发送");
            return;
        }

        try {
            System.out.println("[定时任务] 开始发送操作日志日报...");

            // 计算时间范围：前天20:00 至 当天20:00
            LocalDateTime endTime = LocalDateTime.now().withHour(20).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime startTime = endTime.minusDays(2).withHour(20).withMinute(0).withSecond(0).withNano(0);

            // 查询操作日志
            LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
            wrapper.between(SysOperationLog::getCreateTime, startTime, endTime);
            wrapper.orderByDesc(SysOperationLog::getCreateTime);
            List<SysOperationLog> logs = operationLogService.list(wrapper);

            // 生成汇总报告
            String subject = buildSubject(startTime, endTime);
            String htmlContent = buildHtmlReport(logs, startTime, endTime);

            // 发送邮件
            emailService.sendHtmlEmail(logEmailTo, subject, htmlContent);

            System.out.println("[定时任务] 操作日志日报发送成功，共 " + logs.size() + " 条记录");

        } catch (Exception e) {
            System.err.println("[定时任务] 操作日志日报发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildSubject(LocalDateTime startTime, LocalDateTime endTime) {
        return String.format("【操作日志日报】%s 至 %s",
                startTime.format(DATE_FORMATTER),
                endTime.format(DATE_FORMATTER));
    }

    private String buildHtmlReport(List<SysOperationLog> logs, LocalDateTime startTime, LocalDateTime endTime) {
        long total = logs.size();
        long successCount = logs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long failCount = total - successCount;

        // 按模块统计
        Map<String, Long> moduleStats = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getModule() != null ? l.getModule() : "未知",
                        Collectors.counting()));

        // 按操作类型统计
        Map<String, Long> typeStats = logs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getOperationType() != null ? l.getOperationType() : "OTHER",
                        Collectors.counting()));

        // 最近10条操作
        List<SysOperationLog> recentLogs = logs.stream().limit(10).collect(Collectors.toList());

        // 失败操作列表
        List<SysOperationLog> failLogs = logs.stream()
                .filter(l -> "FAIL".equals(l.getStatus()))
                .limit(20)
                .collect(Collectors.toList());

        StringBuilder html = new StringBuilder();
        html.append("""
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 700px; margin: 0 auto; background: #f8fafc; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 24px; border-radius: 12px 12px 0 0;">
                        <h2 style="margin: 0; font-size: 22px;">Tiamo AI 操作日志日报</h2>
                        <p style="margin: 8px 0 0; opacity: 0.9; font-size: 14px;">统计周期：%s 至 %s</p>
                    </div>
                    <div style="background: white; padding: 24px; border-radius: 0 0 12px 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                """.formatted(startTime.format(FORMATTER), endTime.format(FORMATTER)));

        // 概览统计卡片
        html.append("""
                <div style="display: flex; gap: 12px; margin-bottom: 24px;">
                    <div style="flex: 1; background: #eff6ff; padding: 16px; border-radius: 8px; text-align: center;">
                        <div style="font-size: 28px; font-weight: bold; color: #2563eb;">%d</div>
                        <div style="font-size: 12px; color: #64748b; margin-top: 4px;">总操作数</div>
                    </div>
                    <div style="flex: 1; background: #ecfdf5; padding: 16px; border-radius: 8px; text-align: center;">
                        <div style="font-size: 28px; font-weight: bold; color: #059669;">%d</div>
                        <div style="font-size: 12px; color: #64748b; margin-top: 4px;">成功</div>
                    </div>
                    <div style="flex: 1; background: #fef2f2; padding: 16px; border-radius: 8px; text-align: center;">
                        <div style="font-size: 28px; font-weight: bold; color: #dc2626;">%d</div>
                        <div style="font-size: 12px; color: #64748b; margin-top: 4px;">失败</div>
                    </div>
                </div>
                """.formatted(total, successCount, failCount));

        // 按模块统计
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">📊 按模块统计</h3>
                <table style="width: 100%%; border-collapse: collapse; font-size: 13px; margin-bottom: 20px;">
                """);
        for (Map.Entry<String, Long> entry : moduleStats.entrySet()) {
            html.append("""
                    <tr>
                        <td style="padding: 8px 0; color: #475569; border-bottom: 1px solid #f1f5f9;">%s</td>
                        <td style="padding: 8px 0; color: #1e293b; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%d 次</td>
                    </tr>
                    """.formatted(entry.getKey(), entry.getValue()));
        }
        html.append("</table>");

        // 按操作类型统计
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">🏷️ 按操作类型统计</h3>
                <table style="width: 100%%; border-collapse: collapse; font-size: 13px; margin-bottom: 20px;">
                """);
        for (Map.Entry<String, Long> entry : typeStats.entrySet()) {
            html.append("""
                    <tr>
                        <td style="padding: 8px 0; color: #475569; border-bottom: 1px solid #f1f5f9;">%s</td>
                        <td style="padding: 8px 0; color: #1e293b; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%d 次</td>
                    </tr>
                    """.formatted(entry.getKey(), entry.getValue()));
        }
        html.append("</table>");

        // 最近操作记录
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">📋 最近操作记录（前10条）</h3>
                <div style="overflow-x: auto; margin-bottom: 20px;">
                    <table style="width: 100%%; border-collapse: collapse; font-size: 12px;">
                        <thead>
                            <tr style="background: #f8fafc;">
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">时间</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">模块</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">操作</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">操作人</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">状态</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        for (SysOperationLog log : recentLogs) {
            String statusColor = "SUCCESS".equals(log.getStatus()) ? "#059669" : "#dc2626";
            String statusText = "SUCCESS".equals(log.getStatus()) ? "成功" : "失败";
            html.append("""
                    <tr>
                        <td style="padding: 6px 8px; color: #64748b; border-bottom: 1px solid #f1f5f9; white-space: nowrap;">%s</td>
                        <td style="padding: 6px 8px; color: #475569; border-bottom: 1px solid #f1f5f9;">%s</td>
                        <td style="padding: 6px 8px; color: #1e293b; border-bottom: 1px solid #f1f5f9;">%s</td>
                        <td style="padding: 6px 8px; color: #475569; border-bottom: 1px solid #f1f5f9;">%s</td>
                        <td style="padding: 6px 8px; border-bottom: 1px solid #f1f5f9;"><span style="color: %s; font-weight: 600;">%s</span></td>
                    </tr>
                    """.formatted(
                    log.getCreateTime() != null ? log.getCreateTime().format(FORMATTER) : "-",
                    log.getModule() != null ? log.getModule() : "-",
                    log.getDescription() != null ? log.getDescription() : "-",
                    log.getUsername() != null ? log.getUsername() : "匿名",
                    statusColor,
                    statusText
            ));
        }
        html.append("</tbody></table></div>");

        // 失败操作详情
        if (!failLogs.isEmpty()) {
            html.append("""
                    <h3 style="font-size: 16px; color: #dc2626; margin: 0 0 12px;">⚠️ 失败操作详情（前20条）</h3>
                    <div style="background: #fef2f2; padding: 12px; border-radius: 8px; margin-bottom: 20px;">
                    """);
            for (SysOperationLog log : failLogs) {
                html.append("""
                        <div style="padding: 8px 0; border-bottom: 1px solid #fecaca;">
                            <div style="font-size: 12px; color: #991b1b;"><strong>%s</strong> - %s (%s)</div>
                            <div style="font-size: 11px; color: #7f1d1d; margin-top: 4px; font-family: monospace;">错误: %s</div>
                        </div>
                        """.formatted(
                        log.getCreateTime() != null ? log.getCreateTime().format(FORMATTER) : "-",
                        log.getModule() != null ? log.getModule() : "-",
                        log.getDescription() != null ? log.getDescription() : "-",
                        log.getErrorMsg() != null ? log.getErrorMsg() : "未知错误"
                ));
            }
            html.append("</div>");
        }

        // 页脚
        html.append("""
                <div style="margin-top: 20px; padding-top: 16px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8;">
                    <p style="margin: 0;">此邮件由 Tiamo AI 系统自动发送，每天20:00定时推送。</p>
                    <p style="margin: 4px 0 0;">如需关闭通知，请修改 application.yml 中 app.log-email.enabled 为 false。</p>
                </div>
                </div>
                </div>
                """);

        return html.toString();
    }
}
