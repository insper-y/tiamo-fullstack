package com.tiamo.controller;

import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.entity.ChatMessage;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.ChatService;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 聊天 Controller
 * 提供消息发送、聊天记录、会话列表等接口
 * 所有接口路径: /api/chat/**
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

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
     * 发送消息
     * POST /api/chat/send
     */
    @PostMapping("/send")
    @OperationLog(module = "聊天消息", description = "发送聊天消息", operationType = "CREATE")
    public Result<Map<String, Object>> sendMessage(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> request) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        Long toUserId = request.get("toUserId") != null ? Long.valueOf(request.get("toUserId").toString()) : null;
        String content = (String) request.get("content");
        String msgType = (String) request.get("msgType");

        if (toUserId == null) {
            return new Result<>(400, null, "请指定接收者");
        }
        if (content == null || content.trim().isEmpty()) {
            return new Result<>(400, null, "消息内容不能为空");
        }

        Map<String, Object> result = chatService.sendMessage(
                currentUser.getId(), currentUser.getUsername(), toUserId, content, msgType);
        if ((Boolean) result.get("success")) {
            return new Result<>(200, result, (String) result.get("msg"));
        }
        return new Result<>(400, null, (String) result.get("msg"));
    }

    /**
     * 获取与某个好友的聊天记录
     * GET /api/chat/history?friendId=xxx&page=1&size=20
     */
    @GetMapping("/history")
    public Result<List<ChatMessage>> getChatHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam Long friendId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        List<ChatMessage> list = chatService.getChatHistory(currentUser.getId(), friendId, page, size);
        return new Result<>(200, list, "获取成功");
    }

    /**
     * 获取会话列表
     * GET /api/chat/conversations
     */
    @GetMapping("/conversations")
    public Result<List<Map<String, Object>>> getConversations(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        List<Map<String, Object>> list = chatService.getConversationList(currentUser.getId());
        return new Result<>(200, list, "获取成功");
    }

    /**
     * 获取未读消息总数
     * GET /api/chat/unread-count
     */
    @GetMapping("/unread-count")
    public Result<Map<String, Object>> getUnreadCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        int count = chatService.getUnreadCount(currentUser.getId());
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("count", count);
        return new Result<>(200, data, "获取成功");
    }

    /**
     * 获取与某个好友的未读消息数
     * GET /api/chat/unread-count?friendId=xxx
     */
    @GetMapping("/unread-count-with-friend")
    public Result<Map<String, Object>> getUnreadCountWithFriend(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam Long friendId) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        int count = chatService.getUnreadCountWithFriend(currentUser.getId(), friendId);
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("count", count);
        return new Result<>(200, data, "获取成功");
    }

    /**
     * 标记与某个好友的消息为已读
     * PUT /api/chat/mark-read?friendId=xxx
     */
    @PutMapping("/mark-read")
    @OperationLog(module = "聊天消息", description = "标记消息已读", operationType = "UPDATE")
    public Result<Map<String, Object>> markAsRead(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam Long friendId) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        Map<String, Object> result = chatService.markAsRead(currentUser.getId(), friendId);
        return new Result<>(200, result, (String) result.get("msg"));
    }

    /**
     * 获取新消息（轮询用）
     * GET /api/chat/new-messages?lastMessageId=xxx
     */
    @GetMapping("/new-messages")
    public Result<List<ChatMessage>> getNewMessages(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(defaultValue = "0") Long lastMessageId) {
        SysUser currentUser = getCurrentUser(authHeader);
        if (currentUser == null) {
            return new Result<>(401, null, "请先登录");
        }
        List<ChatMessage> list = chatService.getNewMessages(currentUser.getId(), lastMessageId);
        return new Result<>(200, list, "获取成功");
    }
}
