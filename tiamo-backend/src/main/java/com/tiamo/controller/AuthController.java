package com.tiamo.controller;
import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.dto.LoginDTO;
import com.tiamo.dto.LoginResponse;
import com.tiamo.dto.RegisterDTO;
import com.tiamo.dto.ResetPasswordDTO;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.entity.SysUser;
import com.tiamo.security.CaptchaService;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.SysOperationLogService;
import com.tiamo.service.impl.SysUserServiceImpl;
import com.tiamo.service.InviteCodeService;
import com.tiamo.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
/**
 * 认证 Controller
 * 提供登录、注册、发送验证码、重置密码等接口
 * 所有接口路径: /api/auth/**
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @Autowired
    private SysUserServiceImpl userService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private CaptchaService captchaService;
    @Autowired
    private SysOperationLogService operationLogService;
    @Autowired
    private InviteCodeService inviteCodeService;
    @Autowired
    private EmailService emailService;
    /**
     * 用户登录
     * POST /api/auth/login
     */
    @PostMapping("/login")
    @OperationLog(module = "认证管理", description = "用户登录", operationType = "LOGIN")
    public Result<LoginResponse> login(@RequestBody LoginDTO loginDTO) {
        // 参数校验
        if (loginDTO.getUsername() == null || loginDTO.getUsername().trim().isEmpty()) {
            return new Result<>(400, null, "请输入用户名");
        }
        if (loginDTO.getPassword() == null || loginDTO.getPassword().isEmpty()) {
            return new Result<>(400, null, "请输入密码");
        }
        String username = loginDTO.getUsername().trim();
        // 查询用户（支持用户名或邮箱登录）
        SysUser user = userService.getByUsername(username);
        if (user == null && username.contains("@")) {
            // 如果输入包含@，尝试用邮箱查询
            user = userService.getByEmail(username);
        }
        if (user == null) {
            recordLoginFail(username, "用户不存在", null);
            return new Result<>(401, null, "用户名或密码错误");
        }
        // 检查状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            recordLoginFail(username, "账号已被禁用", user.getId());
            return new Result<>(403, null, "账号已被禁用，请联系管理员");
        }
        // 校验密码
        if (!userService.checkPassword(loginDTO.getPassword(), user.getPassword())) {
            recordLoginFail(username, "密码错误", user.getId());
            return new Result<>(401, null, "用户名或密码错误");
        }
        // 生成 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        // 构建响应
        LoginResponse response = new LoginResponse(
                token,
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getEmail(),
                user.getRole() != null ? user.getRole() : 0
        );
        return new Result<>(200, response, "登录成功");
    }

    /**
     * 记录登录失败日志
     */
    private void recordLoginFail(String username, String reason, Long userId) {
        try {
            SysOperationLog log = new SysOperationLog();
            log.setUserId(userId);
            log.setUsername(username);
            log.setModule("认证管理");
            log.setDescription("登录失败: " + reason);
            log.setOperationType("LOGIN");
            log.setStatus("FAIL");
            log.setErrorMsg(reason);
            log.setCreateTime(LocalDateTime.now());
            // 获取请求IP
            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    log.setIp(request.getRemoteAddr());
                    log.setUrl(request.getRequestURI());
                    log.setMethod("AuthController.login");
                }
            } catch (Exception ignored) {}
            operationLogService.save(log);
        } catch (Exception e) {
            System.out.println("[登录日志] 记录失败: " + e.getMessage());
        }
    }
    /**
     * 用户注册
     * POST /api/auth/register
     */
    @PostMapping("/register")
    @OperationLog(module = "认证管理", description = "用户注册", operationType = "CREATE")
    public Result<String> register(@RequestBody RegisterDTO registerDTO) {
        // 参数校验
        if (registerDTO.getUsername() == null || registerDTO.getUsername().trim().isEmpty()) {
            return new Result<>(400, null, "请输入用户名");
        }
        if (!registerDTO.getUsername().matches("^[a-zA-Z0-9_]{4,20}$")) {
            return new Result<>(400, null, "用户名需为4-20位字母、数字或下划线");
        }
        if (registerDTO.getInviteCode() == null || registerDTO.getInviteCode().trim().isEmpty()) {
            return new Result<>(400, null, "请输入邀请码");
        }
        // 验证邀请码（3分钟有效，使用后删除）
        if (!inviteCodeService.validateCode(registerDTO.getInviteCode().trim())) {
            return new Result<>(400, null, "邀请码无效或已过期（有效期3分钟）");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().length() < 8) {
            return new Result<>(400, null, "密码至少8位");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return new Result<>(400, null, "两次输入的密码不一致");
        }
        // 检查用户名是否已存在
        if (userService.getByUsername(registerDTO.getUsername()) != null) {
            return new Result<>(400, null, "用户名已被注册");
        }
        // 创建用户（默认普通用户）
        SysUser user = new SysUser();
        user.setUsername(registerDTO.getUsername().trim());
        user.setPassword(registerDTO.getPassword());
        user.setEmail(registerDTO.getEmail());
        user.setPhone(registerDTO.getPhone());
        user.setNickname(registerDTO.getUsername());
        user.setRole(0);
        boolean success = userService.register(user);
        if (success) {
            return new Result<>(200, null, "注册成功");
        }
        return new Result<>(500, null, "注册失败，请稍后重试");
    }
    /**
     * 发送验证码
     * POST /api/auth/send-captcha
     */
    @PostMapping("/send-captcha")
    public Result<Map<String, String>> sendCaptcha(@RequestBody Map<String, String> request) {
        String phone = request.get("phone");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) {
            return new Result<>(400, null, "请输入有效的手机号");
        }
        String captcha = captchaService.generateCaptcha(phone);
        // 演示环境返回验证码，生产环境不要返回
        Map<String, String> data = new HashMap<>();
        data.put("phone", phone);
        data.put("captcha", captcha); // 仅开发环境，生产环境删除此行
        return new Result<>(200, data, "验证码已发送，5分钟内有效");
    }

    /**
     * 发送邮箱验证码（忘记密码用）
     * POST /api/auth/send-email-captcha
     */
    @PostMapping("/send-email-captcha")
    public Result<Map<String, String>> sendEmailCaptcha(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return new Result<>(400, null, "请输入有效的邮箱地址");
        }
        SysUser user = userService.getByEmail(email);
        if (user == null) {
            return new Result<>(400, null, "该邮箱未注册");
        }
        String captcha = captchaService.generateCaptcha(email);
        try {
            String subject = "【Tiamo AI】找回密码验证码";
            String htmlContent = "<div style='font-family:Arial,sans-serif;max-width:500px;margin:0 auto;'>"
                + "<div style='background:linear-gradient(135deg,#6366f1,#8b5cf6);color:white;padding:24px;border-radius:12px 12px 0 0;'>"
                + "<h2 style='margin:0;font-size:20px;'>找回密码验证码</h2></div>"
                + "<div style='background:white;padding:24px;border-radius:0 0 12px 12px;box-shadow:0 4px 12px rgba(0,0,0,0.1);'>"
                + "<p style='font-size:14px;color:#475569;margin:0 0 16px;'>您好，您正在进行找回密码操作，验证码如下：</p>"
                + "<div style='text-align:center;padding:20px;background:#f1f5f9;border-radius:8px;margin-bottom:16px;'>"
                + "<span style='font-size:36px;font-weight:bold;letter-spacing:8px;color:#6366f1;font-family:monospace;'>" + captcha + "</span></div>"
                + "<p style='font-size:12px;color:#94a3b8;margin:0;'>验证码5分钟内有效，请勿泄露给他人。</p></div></div>";
            emailService.sendHtmlEmail(email, subject, htmlContent);
        } catch (Exception e) {
            return new Result<>(500, null, "邮件发送失败，请稍后重试");
        }
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        return new Result<>(200, data, "验证码已发送到您的邮箱，5分钟内有效");
    }

    /**
     * 验证邮箱验证码（第一步验证用）
     * POST /api/auth/verify-email-captcha
     */
    @PostMapping("/verify-email-captcha")
    public Result<String> verifyEmailCaptcha(@RequestBody Map<String, String> request) {
        String email = request.get("email");
        String captcha = request.get("captcha");
        
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return new Result<>(400, null, "请输入有效的邮箱地址");
        }
        if (captcha == null || captcha.length() != 6) {
            return new Result<>(400, null, "请输入6位验证码");
        }
        // 检查邮箱是否已注册
        SysUser user = userService.getByEmail(email);
        if (user == null) {
            return new Result<>(400, null, "该邮箱未注册");
        }
        // 校验验证码（注意：这里不删除验证码，因为第二步重置密码时还需要用）
        // 所以需要添加一个只验证不删除的方法
        if (!captchaService.validateCaptchaOnly(email, captcha)) {
            return new Result<>(400, null, "验证码错误或已过期");
        }
        return new Result<>(200, null, "验证码验证成功");
    }
    /**
     * 重置密码（忘记密码）
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    @OperationLog(module = "认证管理", description = "重置密码", operationType = "UPDATE")
    public Result<String> resetPassword(@RequestBody ResetPasswordDTO resetDTO) {
        // 参数校验：支持邮箱或手机号
        boolean useEmail = resetDTO.getEmail() != null && !resetDTO.getEmail().trim().isEmpty();
        String account = useEmail ? resetDTO.getEmail().trim() : resetDTO.getPhone();
        
        if (useEmail) {
            if (!account.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                return new Result<>(400, null, "请输入有效的邮箱地址");
            }
        } else {
            if (account == null || !account.matches("^1[3-9]\\d{9}$")) {
                return new Result<>(400, null, "请输入有效的手机号或邮箱");
            }
        }
        if (resetDTO.getCaptcha() == null || resetDTO.getCaptcha().length() != 6) {
            return new Result<>(400, null, "请输入6位验证码");
        }
        if (resetDTO.getNewPassword() == null || resetDTO.getNewPassword().length() < 8) {
            return new Result<>(400, null, "新密码至少8位");
        }
        if (!resetDTO.getNewPassword().equals(resetDTO.getConfirmPassword())) {
            return new Result<>(400, null, "两次输入的密码不一致");
        }
        // 检查账号是否已注册
        SysUser user = useEmail ? userService.getByEmail(account) : userService.getByPhone(account);
        if (user == null) {
            return new Result<>(400, null, useEmail ? "该邮箱未注册" : "该手机号未注册");
        }
        // 校验验证码
        if (!captchaService.validateCaptcha(account, resetDTO.getCaptcha())) {
            return new Result<>(400, null, "验证码错误或已过期");
        }
        // 重置密码
        boolean success = useEmail ? userService.resetPasswordByEmail(account, resetDTO.getNewPassword()) 
                                   : userService.resetPasswordByPhone(account, resetDTO.getNewPassword());
        if (success) {
            return new Result<>(200, null, "密码重置成功，请使用新密码登录");
        }
        return new Result<>(500, null, "密码重置失败，请稍后重试");
    }
    /**
     * 验证 Token 是否有效
     * GET /api/auth/verify
     */
    @GetMapping("/verify")
    public Result<Map<String, Object>> verifyToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null || !jwtUtil.validateToken(token)) {
            return new Result<>(401, null, "Token无效或已过期");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("userId", jwtUtil.getUserIdFromToken(token));
        data.put("username", jwtUtil.getUsernameFromToken(token));
        data.put("valid", true);
        return new Result<>(200, data, "Token有效");
    }
}
