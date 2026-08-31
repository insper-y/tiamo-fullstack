package com.tiamo.controller;
import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
/**
 * 管理员 Controller
 * 提供用户管理等接口，仅管理员可访问
 * 所有接口路径: /api/admin/**
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    @Autowired
    private SysUserServiceImpl userService;
    @Autowired
    private JwtUtil jwtUtil;
    /**
     * 获取用户列表（仅管理员）
     * GET /api/admin/users
     */
    @GetMapping("/users")
    @OperationLog(module = "用户管理", description = "获取用户列表", operationType = "QUERY")
    public Result<List<Map<String, Object>>> getUserList(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 验证管理员权限
        SysUser currentUser = getAdminUser(authHeader);
        if (currentUser == null) {
            return new Result<>(403, null, "无权限访问，仅管理员可操作");
        }
        // 查询所有用户
        List<SysUser> users = userService.list();
        // 转换为安全的响应格式（不返回密码）
        List<Map<String, Object>> userList = users.stream().map(user -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("nickname", user.getNickname());
            map.put("email", user.getEmail());
            map.put("phone", user.getPhone());
            map.put("role", user.getRole() != null ? user.getRole() : 0);
            map.put("roleName", (user.getRole() != null && user.getRole() == 1) ? "管理员" : "普通用户");
            map.put("status", user.getStatus());
            map.put("statusName", (user.getStatus() != null && user.getStatus() == 1) ? "启用" : "禁用");
            map.put("createTime", user.getCreateTime());
            return map;
        }).collect(Collectors.toList());
        return new Result<>(200, userList, "查询成功");
    }
    /**
     * 从 Token 中获取用户并验证是否为管理员
     */
    private SysUser getAdminUser(String authHeader) {
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return null;
        }
        SysUser user = userService.getById(userId);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            return null;
        }
        return user;
    }
}
