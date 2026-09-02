package com.tiamo.controller;

import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.entity.FriendRequest;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.FriendService;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 好友管理 Controller
 * 提供好友请求、好友列表等接口
 * 所有接口路径: /api/friend/**
 */
@RestController
@RequestMapping("/api/friend")
public class FriendController {

    @Autowired
    private FriendService friendService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserServiceImpl userService;

    /**
     * 从Token中获取当前用户
     */
    private SysUser getCurrentUser(String authHeader) {
        String token = jwtUtil.extractTokenFromHeader(authHeader);
        if (token == null || !jwtUtil.validateToken(token)) {
            return null;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return null;
        }
        return userService.getById(userId);
    }

    /**
     * 发送好友请求
     * POST /api/friend/request
     */
    @PostMapping("/request")
    @OperationLog(module = "好友管理", description = "发送好友请求", operationType = "CREATE")
    public Result<Map<String, Object>> sendFriendRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> request) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        String toUsername = request.get("toUsername");
        String message = request.get("message");
        if (toUsername == null || toUsername.trim().isEmpty()) {
            return new Result<>(400, null, "请输入对方用户名");
        }
        Map<String, Object> result = friendService.sendFriendRequest(
                currentUser.getId(), currentUser.getUsername(), toUsername.trim(), message);
        if ((Boolean) result.get("success")) {
            return new Result<>(200, result, (String) result.get("msg"));
        }
        return new Result<>(400, null, (String) result.get("msg"));
    }

    /**
     * 接受好友请求
     * PUT /api/friend/request/{id}/accept
     */
    @PutMapping("/request/{id}/accept")
    @OperationLog(module = "好友管理", description = "接受好友请求", operationType = "UPDATE")
    public Result<Map<String, Object>> acceptFriendRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        Map<String, Object> result = friendService.acceptFriendRequest(id, currentUser.getId());
        if ((Boolean) result.get("success")) {
            return new Result<>(200, result, (String) result.get("msg"));
        }
        return new Result<>(400, null, (String) result.get("msg"));
    }

    /**
     * 拒绝好友请求
     * PUT /api/friend/request/{id}/reject
     */
    @PutMapping("/request/{id}/reject")
    @OperationLog(module = "好友管理", description = "拒绝好友请求", operationType = "UPDATE")
    public Result<Map<String, Object>> rejectFriendRequest(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        Map<String, Object> result = friendService.rejectFriendRequest(id, currentUser.getId());
        if ((Boolean) result.get("success")) {
            return new Result<>(200, result, (String) result.get("msg"));
        }
        return new Result<>(400, null, (String) result.get("msg"));
    }

    /**
     * 获取好友列表
     * GET /api/friend/list
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> getFriendList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        List<Map<String, Object>> list = friendService.getFriendList(currentUser.getId());
        return new Result<>(200, list, "获取成功");
    }

    /**
     * 获取收到的好友请求
     * GET /api/friend/requests/received
     */
    @GetMapping("/requests/received")
    public Result<List<FriendRequest>> getReceivedRequests(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        List<FriendRequest> list = friendService.getReceivedRequests(currentUser.getId());
        return new Result<>(200, list, "获取成功");
    }

    /**
     * 获取发送的好友请求
     * GET /api/friend/requests/sent
     */
    @GetMapping("/requests/sent")
    public Result<List<FriendRequest>> getSentRequests(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        List<FriendRequest> list = friendService.getSentRequests(currentUser.getId());
        return new Result<>(200, list, "获取成功");
    }

    /**
     * 获取待处理请求数量
     * GET /api/friend/requests/pending-count
     */
    @GetMapping("/requests/pending-count")
    public Result<Map<String, Object>> getPendingCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        int count = friendService.getPendingRequestCount(currentUser.getId());
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("count", count);
        return new Result<>(200, data, "获取成功");
    }

    /**
     * 删除好友
     * DELETE /api/friend/{friendId}
     */
    @DeleteMapping("/{friendId}")
    @OperationLog(module = "好友管理", description = "删除好友", operationType = "DELETE")
    public Result<Map<String, Object>> deleteFriend(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long friendId) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        Map<String, Object> result = friendService.deleteFriend(currentUser.getId(), friendId);
        if ((Boolean) result.get("success")) {
            return new Result<>(200, result, (String) result.get("msg"));
        }
        return new Result<>(400, null, (String) result.get("msg"));
    }
}
