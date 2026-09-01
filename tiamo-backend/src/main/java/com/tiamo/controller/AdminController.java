package com.tiamo.controller;

import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
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
    public Result<List<Map<String, Object>>> getUserList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getAdminUser(authHeader);
        if (currentUser == null) {
            return new Result<>(403, null, "无权限访问，仅管理员可操作");
        }
        List<SysUser> users = userService.list();
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
     * 设置用户角色（仅管理员）
     * PUT /api/admin/users/{id}/role
     * body: {"role": 0} 0-普通用户 1-管理员
     */
    @PutMapping("/users/{id}/role")
    @OperationLog(module = "用户管理", description = "设置用户角色", operationType = "UPDATE")
    public Result<String> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getAdminUser(authHeader);
        if (currentUser == null) {
            return new Result<>(403, null, "无权限操作，仅管理员可操作");
        }
        Integer role = request.get("role");
        if (role == null || (role != 0 && role != 1)) {
            return new Result<>(400, null, "角色参数无效，0-普通用户 1-管理员");
        }
        // 不允许修改自己的角色
        if (currentUser.getId().equals(id)) {
            return new Result<>(400, null, "不能修改自己的角色");
        }
        SysUser user = userService.getById(id);
        if (user == null) {
            return new Result<>(404, null, "用户不存在");
        }
        user.setRole(role);
        user.setUpdateTime(LocalDateTime.now());
        boolean flag = userService.updateById(user);
        if (flag) {
            String roleName = role == 1 ? "管理员" : "普通用户";
            return new Result<>(200, null, "已将用户 " + user.getUsername() + " 设置为" + roleName);
        }
        return new Result<>(500, null, "设置失败");
    }

    /**
     * 设置用户状态（仅管理员）
     * PUT /api/admin/users/{id}/status
     * body: {"status": 1} 0-禁用 1-启用
     */
    @PutMapping("/users/{id}/status")
    @OperationLog(module = "用户管理", description = "设置用户状态", operationType = "UPDATE")
    public Result<String> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getAdminUser(authHeader);
        if (currentUser == null) {
            return new Result<>(403, null, "无权限操作，仅管理员可操作");
        }
        Integer status = request.get("status");
        if (status == null || (status != 0 && status != 1)) {
            return new Result<>(400, null, "状态参数无效，0-禁用 1-启用");
        }
        // 不允许禁用自己
        if (currentUser.getId().equals(id)) {
            return new Result<>(400, null, "不能修改自己的状态");
        }
        SysUser user = userService.getById(id);
        if (user == null) {
            return new Result<>(404, null, "用户不存在");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        boolean flag = userService.updateById(user);
        if (flag) {
            String statusName = status == 1 ? "启用" : "禁用";
            return new Result<>(200, null, "已将用户 " + user.getUsername() + " " + statusName);
        }
        return new Result<>(500, null, "设置失败");
    }

    /**
     * 删除用户（仅管理员，只能删除普通用户）
     * DELETE /api/admin/users/{id}
     */
    @DeleteMapping("/users/{id}")
    @OperationLog(module = "用户管理", description = "删除用户", operationType = "DELETE")
    public Result<String> deleteUser(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getAdminUser(authHeader);
        if (currentUser == null) {
            return new Result<>(403, null, "无权限操作，仅管理员可操作");
        }
        // 不允许删除自己
        if (currentUser.getId().equals(id)) {
            return new Result<>(400, null, "不能删除自己");
        }
        SysUser user = userService.getById(id);
        if (user == null) {
            return new Result<>(404, null, "用户不存在");
        }
        // 不允许删除管理员
        if (user.getRole() != null && user.getRole() == 1) {
            return new Result<>(400, null, "不能删除管理员账户");
        }
        boolean flag = userService.removeById(id);
        if (flag) {
            return new Result<>(200, null, "已删除用户 " + user.getUsername());
        }
        return new Result<>(500, null, "删除失败");
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
