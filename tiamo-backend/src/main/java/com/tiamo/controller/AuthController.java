package com.tiamo.controller;
import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.dto.LoginDTO;
import com.tiamo.dto.LoginResponse;
import com.tiamo.dto.RegisterDTO;
import com.tiamo.dto.ResetPasswordDTO;
import com.tiamo.entity.SysUser;
import com.tiamo.security.CaptchaService;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
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
        // 查询用户
        SysUser user = userService.getByUsername(loginDTO.getUsername().trim());
        if (user == null) {
            return new Result<>(401, null, "用户名或密码错误");
        }
        // 检查状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            return new Result<>(403, null, "账号已被禁用，请联系管理员");
        }
        // 校验密码
        if (!userService.checkPassword(loginDTO.getPassword(), user.getPassword())) {
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
        if (registerDTO.getPhone() == null || !registerDTO.getPhone().matches("^1[3-9]\\d{9}$")) {
            return new Result<>(400, null, "请输入有效的手机号");
        }
        if (registerDTO.getCaptcha() == null || registerDTO.getCaptcha().length() != 6) {
            return new Result<>(400, null, "请输入6位验证码");
        }
        if (registerDTO.getPassword() == null || registerDTO.getPassword().length() < 8) {
            return new Result<>(400, null, "密码至少8位");
        }
        if (!registerDTO.getPassword().equals(registerDTO.getConfirmPassword())) {
            return new Result<>(400, null, "两次输入的密码不一致");
        }
        // 校验验证码
        if (!captchaService.validateCaptcha(registerDTO.getPhone(), registerDTO.getCaptcha())) {
            return new Result<>(400, null, "验证码错误或已过期");
        }
        // 检查用户名是否已存在
        if (userService.getByUsername(registerDTO.getUsername()) != null) {
            return new Result<>(400, null, "用户名已被注册");
        }
        // 检查手机号是否已注册
        if (userService.getByPhone(registerDTO.getPhone()) != null) {
            return new Result<>(400, null, "该手机号已被注册");
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
     * 重置密码（忘记密码）
     * POST /api/auth/reset-password
     */
    @PostMapping("/reset-password")
    @OperationLog(module = "认证管理", description = "重置密码", operationType = "UPDATE")
    public Result<String> resetPassword(@RequestBody ResetPasswordDTO resetDTO) {
        // 参数校验
        if (resetDTO.getPhone() == null || !resetDTO.getPhone().matches("^1[3-9]\\d{9}$")) {
            return new Result<>(400, null, "请输入有效的手机号");
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
        // 检查手机号是否已注册
        SysUser user = userService.getByPhone(resetDTO.getPhone());
        if (user == null) {
            return new Result<>(400, null, "该手机号未注册");
        }
        // 校验验证码
        if (!captchaService.validateCaptcha(resetDTO.getPhone(), resetDTO.getCaptcha())) {
            return new Result<>(400, null, "验证码错误或已过期");
        }
        // 重置密码
        boolean success = userService.resetPasswordByPhone(resetDTO.getPhone(), resetDTO.getNewPassword());
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
