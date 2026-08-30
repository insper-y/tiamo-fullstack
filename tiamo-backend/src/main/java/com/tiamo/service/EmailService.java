package com.tiamo.service;

import com.tiamo.entity.SysOperationLog;

/**
 * 邮件服务接口
 */
public interface EmailService {

    /**
     * 异步发送操作日志邮件
     */
    void sendOperationLogEmailAsync(SysOperationLog log);

    /**
     * 发送简单文本邮件
     */
    void sendSimpleEmail(String to, String subject, String content);

    /**
     * 发送 HTML 邮件
     */
    void sendHtmlEmail(String to, String subject, String htmlContent);
}
