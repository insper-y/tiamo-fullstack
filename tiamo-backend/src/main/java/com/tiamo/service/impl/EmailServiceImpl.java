package com.tiamo.service.impl;

import com.tiamo.entity.SysOperationLog;
import com.tiamo.service.EmailService;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

/**
 * 邮件服务实现
 */
@Service
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@tiamo.com}")
    private String fromEmail;

    @Value("${app.log-email.to:}")
    private String logEmailTo;

    @Value("${app.log-email.enabled:false}")
    private boolean logEmailEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Async("logExecutor")
    public void sendOperationLogEmailAsync(SysOperationLog log) {
        if (!logEmailEnabled || logEmailTo == null || logEmailTo.isEmpty()) {
            return;
        }

        try {
            String subject = buildSubject(log);
            String htmlContent = buildHtmlContent(log);
            sendHtmlEmail(logEmailTo, subject, htmlContent);
        } catch (Exception e) {
            System.err.println("[邮件发送失败] " + e.getMessage());
        }
    }

    @Override
    public void sendSimpleEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
    }

    @Override
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mailSender.createMimeMessage(), true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(helper.getMimeMessage());
        } catch (Exception e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendEmailWithAttachment(String to, String subject, String htmlContent,
                                          byte[] attachment, String attachmentName) {
        try {
            MimeMessageHelper helper = new MimeMessageHelper(
                    mailSender.createMimeMessage(), true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            // 添加附件
            ByteArrayResource resource = new ByteArrayResource(attachment);
            helper.addAttachment(attachmentName, resource);
            mailSender.send(helper.getMimeMessage());
        } catch (Exception e) {
            throw new RuntimeException("带附件邮件发送失败: " + e.getMessage(), e);
        }
    }

    private String buildSubject(SysOperationLog log) {
        String statusIcon = "SUCCESS".equals(log.getStatus()) ? "✅" : "❌";
        return String.format("%s【操作日志】%s - %s",
                statusIcon,
                log.getModule() != null ? log.getModule() : "系统",
                log.getDescription() != null ? log.getDescription() : "未知操作");
    }

    private String buildHtmlContent(SysOperationLog log) {
        String statusColor = "SUCCESS".equals(log.getStatus()) ? "#10b981" : "#ef4444";
        String statusText = "SUCCESS".equals(log.getStatus()) ? "成功" : "失败";

        return """
                <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; max-width: 600px; margin: 0 auto; background: #f8fafc; padding: 20px;">
                    <div style="background: linear-gradient(135deg, #6366f1, #8b5cf6); color: white; padding: 24px; border-radius: 12px 12px 0 0;">
                        <h2 style="margin: 0; font-size: 20px;">Tiamo AI 操作日志通知</h2>
                        <p style="margin: 8px 0 0; opacity: 0.9; font-size: 14px;">系统检测到新的操作行为</p>
                    </div>
                    <div style="background: white; padding: 24px; border-radius: 0 0 12px 12px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
                        <table style="width: 100%%; border-collapse: collapse; font-size: 14px;">
                            <tr>
                                <td style="padding: 10px 0; color: #64748b; width: 100px;">操作模块</td>
                                <td style="padding: 10px 0; color: #1e293b; font-weight: 500;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">操作描述</td>
                                <td style="padding: 10px 0; color: #1e293b; font-weight: 500;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">操作人</td>
                                <td style="padding: 10px 0; color: #1e293b;">%s (ID: %s)</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">操作类型</td>
                                <td style="padding: 10px 0; color: #1e293b;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">请求URL</td>
                                <td style="padding: 10px 0; color: #1e293b; font-family: monospace; font-size: 12px;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">请求IP</td>
                                <td style="padding: 10px 0; color: #1e293b; font-family: monospace;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">执行耗时</td>
                                <td style="padding: 10px 0; color: #1e293b;">%d ms</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">操作时间</td>
                                <td style="padding: 10px 0; color: #1e293b;">%s</td>
                            </tr>
                            <tr>
                                <td style="padding: 10px 0; color: #64748b;">操作结果</td>
                                <td style="padding: 10px 0;"><span style="background: %s; color: white; padding: 4px 12px; border-radius: 20px; font-size: 12px; font-weight: 600;">%s</span></td>
                            </tr>
                        </table>
                        %s
                        <div style="margin-top: 20px; padding-top: 16px; border-top: 1px solid #e2e8f0; font-size: 12px; color: #94a3b8;">
                            <p style="margin: 0;">此邮件由 Tiamo AI 系统自动发送，请勿直接回复。</p>
                            <p style="margin: 4px 0 0;">如需关闭通知，请修改 application.yml 中 app.log-email.enabled 为 false。</p>
                        </div>
                    </div>
                </div>
                """.formatted(
                log.getModule() != null ? log.getModule() : "-",
                log.getDescription() != null ? log.getDescription() : "-",
                log.getUsername() != null ? log.getUsername() : "匿名",
                log.getUserId() != null ? log.getUserId() : "-",
                log.getOperationType() != null ? log.getOperationType() : "OTHER",
                log.getUrl() != null ? log.getUrl() : "-",
                log.getIp() != null ? log.getIp() : "-",
                log.getCostTime() != null ? log.getCostTime() : 0,
                log.getCreateTime() != null ? log.getCreateTime().format(FORMATTER) : "-",
                statusColor,
                statusText,
                buildErrorSection(log)
        );
    }

    private String buildErrorSection(SysOperationLog log) {
        if ("SUCCESS".equals(log.getStatus()) || log.getErrorMsg() == null) {
            return "";
        }
        return """
                <div style="margin-top: 16px; padding: 12px; background: #fef2f2; border-radius: 8px; border-left: 4px solid #ef4444;">
                    <p style="margin: 0 0 6px; font-size: 13px; font-weight: 600; color: #dc2626;">错误信息</p>
                    <p style="margin: 0; font-size: 12px; color: #7f1d1d; font-family: monospace; word-break: break-all;">%s</p>
                </div>
                """.formatted(log.getErrorMsg());
    }
}
