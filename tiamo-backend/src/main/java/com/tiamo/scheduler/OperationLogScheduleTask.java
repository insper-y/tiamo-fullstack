package com.tiamo.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.entity.SysRunLog;
import com.tiamo.service.EmailService;
import com.tiamo.service.SysConfigService;
import com.tiamo.service.SysOperationLogService;
import com.tiamo.service.SysRunLogService;
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
 * 系统日报定时汇总任务
 * 每天指定时间自动发送操作日志+运行日志汇总邮件
 * 统计时间段：前天20:00 至 当天20:00
 */
@Component
public class OperationLogScheduleTask {

    @Autowired
    private SysOperationLogService operationLogService;

    @Autowired
    private SysRunLogService runLogService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SysConfigService configService;

    @Value("${app.log-email.to:}")
    private String logEmailTo;

    @Value("${app.log-email.enabled:false}")
    private boolean logEmailEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 每分钟检查一次，如果当前时间匹配配置的发送时间，则发送日报
     * 发送时间从数据库配置读取，管理员可在界面修改
     */
    @Scheduled(cron = "0 * * * * ?")
    public void sendDailyOperationLogReport() {
        try {
            // 从数据库读取配置
            String sendTime = configService.getConfigValue("email.send.time", "20:00");
            boolean enabled = "true".equalsIgnoreCase(configService.getConfigValue("email.send.enabled", "true"));
            String toEmail = configService.getConfigValue("email.send.to", "");

            if (!enabled) {
                return;
            }
            if (toEmail == null || toEmail.isEmpty()) {
                System.out.println("[定时任务] 收件邮箱未配置，跳过发送");
                return;
            }

            // 检查当前时间是否匹配配置的发送时间
            LocalDateTime now = LocalDateTime.now();
            String currentTime = String.format("%02d:%02d", now.getHour(), now.getMinute());
            if (!currentTime.equals(sendTime)) {
                return;
            }

            System.out.println("[定时任务] 开始发送日报（操作日志+运行日志）...");

            // 计算时间范围：前天20:00 至 当天20:00
            LocalDateTime endTime = now.withHour(20).withMinute(0).withSecond(0).withNano(0);
            LocalDateTime startTime = endTime.minusDays(2).withHour(20).withMinute(0).withSecond(0).withNano(0);

            // 查询操作日志
            LambdaQueryWrapper<SysOperationLog> opWrapper = new LambdaQueryWrapper<>();
            opWrapper.between(SysOperationLog::getCreateTime, startTime, endTime);
            opWrapper.orderByDesc(SysOperationLog::getCreateTime);
            List<SysOperationLog> opLogs = operationLogService.list(opWrapper);

            // 查询运行日志
            LambdaQueryWrapper<SysRunLog> runWrapper = new LambdaQueryWrapper<>();
            runWrapper.between(SysRunLog::getCreateTime, startTime, endTime);
            runWrapper.orderByDesc(SysRunLog::getCreateTime);
            List<SysRunLog> runLogs = runLogService.list(runWrapper);

            // 生成汇总报告
            String subject = buildSubject(startTime, endTime);
            String htmlContent = buildHtmlReport(opLogs, runLogs, startTime, endTime);

            // 发送邮件
            emailService.sendHtmlEmail(toEmail, subject, htmlContent);

            System.out.println("[定时任务] 日报发送成功，操作日志 " + opLogs.size() + " 条，运行日志 " + runLogs.size() + " 条");
        } catch (Exception e) {
            System.err.println("[定时任务] 日报发送失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String buildSubject(LocalDateTime startTime, LocalDateTime endTime) {
        return String.format("【系统日报】操作日志+运行日志 %s 至 %s",
                startTime.format(DATE_FORMATTER),
                endTime.format(DATE_FORMATTER));
    }

    private String buildHtmlReport(List<SysOperationLog> opLogs, List<SysRunLog> runLogs,
                                    LocalDateTime startTime, LocalDateTime endTime) {
        // 操作日志统计
        long opTotal = opLogs.size();
        long opSuccess = opLogs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long opFail = opTotal - opSuccess;

        // 运行日志统计
        long runTotal = runLogs.size();
        long runSuccess = runLogs.stream().filter(l -> "SUCCESS".equals(l.getStatus())).count();
        long runFail = runTotal - runSuccess;
        double avgCostTime = runLogs.stream()
                .filter(l -> l.getCostTime() != null)
                .mapToLong(SysRunLog::getCostTime)
                .average()
                .orElse(0);

        // 操作日志按模块统计
        Map<String, Long> moduleStats = opLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getModule() != null ? l.getModule() : "未知",
                        Collectors.counting()));

        // 操作日志按操作类型统计
        Map<String, Long> typeStats = opLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getOperationType() != null ? l.getOperationType() : "OTHER",
                        Collectors.counting()));

        // 运行日志按类名统计
        Map<String, Long> classStats = runLogs.stream()
                .collect(Collectors.groupingBy(
                        l -> l.getClassName() != null ? l.getClassName() : "未知",
                        Collectors.counting()));

        // 最近10条操作日志
        List<SysOperationLog> recentOpLogs = opLogs.stream().limit(10).collect(Collectors.toList());

        // 最近10条运行日志
        List<SysRunLog> recentRunLogs = runLogs.stream().limit(10).collect(Collectors.toList());

        // 失败操作列表
        List<SysOperationLog> failOpLogs = opLogs.stream()
                .filter(l -> "FAIL".equals(l.getStatus()))
                .limit(10)
                .collect(Collectors.toList());

        // 失败运行日志列表
        List<SysRunLog> failRunLogs = runLogs.stream()
                .filter(l -> "FAIL".equals(l.getStatus()))
                .limit(10)
                .collect(Collectors.toList());

        StringBuilder html = new StringBuilder();

        // 邮件头部
        html.append("""
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 700px; margin: 0 auto; background: #f8fafc; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 24px; border-radius: 12px 12px 0 0;">
                        <h2 style="margin: 0; font-size: 22px;">Tiamo AI 系统日报</h2>
                        <p style="margin: 8px 0 0; opacity: 0.9; font-size: 14px;">统计周期：%s 至 %s</p>
                        <p style="margin: 4px 0 0; opacity: 0.8; font-size: 12px;">包含：操作日志 + 运行日志</p>
                    </div>
                    <div style="background: white; padding: 24px; border-radius: 0 0 12px 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                """.formatted(startTime.format(FORMATTER), endTime.format(FORMATTER)));

        // 概览统计卡片 - 操作日志
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">📊 操作日志概览</h3>
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
                """.formatted(opTotal, opSuccess, opFail));

        // 概览统计卡片 - 运行日志
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">⚙️ 运行日志概览</h3>
                <div style="display: flex; gap: 12px; margin-bottom: 24px;">
                    <div style="flex: 1; background: #f5f3ff; padding: 16px; border-radius: 8px; text-align: center;">
                        <div style="font-size: 28px; font-weight: bold; color: #7c3aed;">%d</div>
                        <div style="font-size: 12px; color: #64748b; margin-top: 4px;">总调用数</div>
                    </div>
                    <div style="flex: 1; background: #ecfdf5; padding: 16px; border-radius: 8px; text-align: center;">
                        <div style="font-size: 28px; font-weight: bold; color: #059669;">%d</div>
                        <div style="font-size: 12px; color: #64748b; margin-top: 4px;">成功</div>
                    </div>
                    <div style="flex: 1; background: #fef2f2; padding: 16px; border-radius: 8px; text-align: center;">
                        <div style="font-size: 28px; font-weight: bold; color: #dc2626;">%d</div>
                        <div style="font-size: 12px; color: #64748b; margin-top: 4px;">失败</div>
                    </div>
                    <div style="flex: 1; background: #fffbeb; padding: 16px; border-radius: 8px; text-align: center;">
                        <div style="font-size: 24px; font-weight: bold; color: #d97706;">%.0fms</div>
                        <div style="font-size: 12px; color: #64748b; margin-top: 4px;">平均耗时</div>
                    </div>
                </div>
                """.formatted(runTotal, runSuccess, runFail, avgCostTime));

        // 操作日志按模块统计
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">📊 操作日志 - 按模块统计</h3>
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

        // 操作日志按操作类型统计
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">🏷️ 操作日志 - 按操作类型统计</h3>
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

        // 运行日志按类名统计（前10）
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">⚙️ 运行日志 - 按类名统计（前10）</h3>
                <table style="width: 100%%; border-collapse: collapse; font-size: 13px; margin-bottom: 20px;">
                """);
        classStats.entrySet().stream().limit(10).forEach(entry -> {
            html.append("""
                    <tr>
                        <td style="padding: 8px 0; color: #475569; border-bottom: 1px solid #f1f5f9; font-family: monospace; font-size: 11px;">%s</td>
                        <td style="padding: 8px 0; color: #1e293b; font-weight: 600; text-align: right; border-bottom: 1px solid #f1f5f9;">%d 次</td>
                    </tr>
                    """.formatted(entry.getKey(), entry.getValue()));
        });
        html.append("</table>");

        // 最近操作日志记录
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">📋 最近操作日志（前10条）</h3>
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
        for (SysOperationLog log : recentOpLogs) {
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

        // 最近运行日志记录
        html.append("""
                <h3 style="font-size: 16px; color: #1e293b; margin: 0 0 12px;">⚙️ 最近运行日志（前10条）</h3>
                <div style="overflow-x: auto; margin-bottom: 20px;">
                    <table style="width: 100%%; border-collapse: collapse; font-size: 12px;">
                        <thead>
                            <tr style="background: #f8fafc;">
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">时间</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">类名</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">方法</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">耗时</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">操作人</th>
                                <th style="padding: 8px; text-align: left; color: #64748b; border-bottom: 2px solid #e2e8f0;">状态</th>
                            </tr>
                        </thead>
                        <tbody>
                """);
        for (SysRunLog log : recentRunLogs) {
            String statusColor = "SUCCESS".equals(log.getStatus()) ? "#059669" : "#dc2626";
            String statusText = "SUCCESS".equals(log.getStatus()) ? "成功" : "失败";
            html.append("""
                    <tr>
                        <td style="padding: 6px 8px; color: #64748b; border-bottom: 1px solid #f1f5f9; white-space: nowrap;">%s</td>
                        <td style="padding: 6px 8px; color: #475569; border-bottom: 1px solid #f1f5f9; font-family: monospace; font-size: 10px;">%s</td>
                        <td style="padding: 6px 8px; color: #1e293b; border-bottom: 1px solid #f1f5f9; font-family: monospace; font-size: 10px;">%s</td>
                        <td style="padding: 6px 8px; color: #d97706; border-bottom: 1px solid #f1f5f9; font-weight: 600;">%sms</td>
                        <td style="padding: 6px 8px; color: #475569; border-bottom: 1px solid #f1f5f9;">%s</td>
                        <td style="padding: 6px 8px; border-bottom: 1px solid #f1f5f9;"><span style="color: %s; font-weight: 600;">%s</span></td>
                    </tr>
                    """.formatted(
                    log.getCreateTime() != null ? log.getCreateTime().format(FORMATTER) : "-",
                    log.getClassName() != null ? log.getClassName() : "-",
                    log.getMethodName() != null ? log.getMethodName() : "-",
                    log.getCostTime() != null ? log.getCostTime() : "-",
                    log.getUsername() != null ? log.getUsername() : "匿名",
                    statusColor,
                    statusText
            ));
        }
        html.append("</tbody></table></div>");

        // 失败操作详情
        if (!failOpLogs.isEmpty()) {
            html.append("""
                    <h3 style="font-size: 16px; color: #dc2626; margin: 0 0 12px;">⚠️ 失败操作详情（前10条）</h3>
                    <div style="background: #fef2f2; padding: 12px; border-radius: 8px; margin-bottom: 20px;">
                    """);
            for (SysOperationLog log : failOpLogs) {
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

        // 失败运行日志详情
        if (!failRunLogs.isEmpty()) {
            html.append("""
                    <h3 style="font-size: 16px; color: #dc2626; margin: 0 0 12px;">⚠️ 失败运行日志详情（前10条）</h3>
                    <div style="background: #fef2f2; padding: 12px; border-radius: 8px; margin-bottom: 20px;">
                    """);
            for (SysRunLog log : failRunLogs) {
                html.append("""
                        <div style="padding: 8px 0; border-bottom: 1px solid #fecaca;">
                            <div style="font-size: 12px; color: #991b1b;"><strong>%s</strong> - %s.%s (%sms)</div>
                            <div style="font-size: 11px; color: #7f1d1d; margin-top: 4px; font-family: monospace; word-break: break-all;">异常: %s</div>
                        </div>
                        """.formatted(
                        log.getCreateTime() != null ? log.getCreateTime().format(FORMATTER) : "-",
                        log.getClassName() != null ? log.getClassName() : "-",
                        log.getMethodName() != null ? log.getMethodName() : "-",
                        log.getCostTime() != null ? log.getCostTime() : "-",
                        log.getException() != null ? log.getException() : "未知异常"
                ));
            }
            html.append("</div>");
        }

        // 页脚
        html.append("""
                <div style="margin-top: 20px; padding-top: 16px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8;">
                    <p style="margin: 0;">此邮件由 Tiamo AI 系统自动发送，包含操作日志和运行日志日报。</p>
                    <p style="margin: 4px 0 0;">如需关闭通知，请在系统"邮件配置"页面关闭"日报邮件通知"。</p>
                </div>
                </div>
                </div>
                """);

        return html.toString();
    }
}
