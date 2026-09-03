package com.tiamo.controller;

import com.tiamo.annotation.OperationLog;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.SysConfigService;
import com.tiamo.service.impl.SysUserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 系统配置管理 Controller
 * 管理员可配置邮件发送时间、开关等
 */
@RestController
@RequestMapping("/api/admin/config")
public class ConfigController {

    @Autowired
    private SysConfigService configService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserServiceImpl userService;

    /**
     * 获取邮件配置
     * GET /api/admin/config/email
     */
    @GetMapping("/email")
    @OperationLog(module = "系统配置", description = "获取邮件配置", operationType = "QUERY")
    public ResponseEntity<Map<String, Object>> getEmailConfig(HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证管理员权限
            if (!isAdmin(request)) {
                result.put("code", 403);
                result.put("msg", "无权限访问，仅管理员可操作");
                return ResponseEntity.status(403).body(result);
            }

            Map<String, String> config = new HashMap<>();
            config.put("sendTime", configService.getConfigValue("email.send.time", "20:00"));
            config.put("enabled", configService.getConfigValue("email.send.enabled", "true"));
            config.put("toEmail", configService.getConfigValue("email.send.to", ""));
            config.put("realTimeEnabled", configService.getConfigValue("email.realtime.enabled", "true"));

            result.put("code", 200);
            result.put("data", config);
            result.put("msg", "获取成功");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "获取配置失败，请稍后重试");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 更新邮件配置
     * PUT /api/admin/config/email
     */
    @PutMapping("/email")
    @OperationLog(module = "系统配置", description = "更新邮件配置", operationType = "UPDATE")
    public ResponseEntity<Map<String, Object>> updateEmailConfig(
            @RequestBody Map<String, String> configData,
            HttpServletRequest request) {
        Map<String, Object> result = new HashMap<>();
        try {
            // 验证管理员权限
            if (!isAdmin(request)) {
                result.put("code", 403);
                result.put("msg", "无权限访问，仅管理员可操作");
                return ResponseEntity.status(403).body(result);
            }

            // 更新配置
            if (configData.containsKey("sendTime")) {
                String sendTime = configData.get("sendTime");
                // 验证时间格式 HH:mm
                if (!sendTime.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    result.put("code", 400);
                    result.put("msg", "时间格式错误，应为 HH:mm，如 20:00");
                    return ResponseEntity.badRequest().body(result);
                }
                configService.setConfigValue("email.send.time", sendTime, "日报发送时间");
            }
            if (configData.containsKey("enabled")) {
                configService.setConfigValue("email.send.enabled", configData.get("enabled"), "日报邮件开关");
            }
            if (configData.containsKey("toEmail")) {
                configService.setConfigValue("email.send.to", configData.get("toEmail"), "收件邮箱");
            }
            if (configData.containsKey("realTimeEnabled")) {
                configService.setConfigValue("email.realtime.enabled", configData.get("realTimeEnabled"), "实时邮件开关");
            }

            result.put("code", 200);
            result.put("msg", "配置更新成功");
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "更新配置失败，请稍后重试");
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 验证是否为管理员
     */
    private boolean isAdmin(HttpServletRequest request) {
        try {
            String token = jwtUtil.extractTokenFromHeader(request.getHeader("Authorization"));
            if (token == null || !jwtUtil.validateToken(token)) {
                return false;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            SysUser user = userService.getById(userId);
            return user != null && user.getRole() != null && user.getRole() == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
