package com.tiamo.controller;

import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Result;
import com.tiamo.entity.RecycleApproval;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.RecycleApprovalService;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 回收站审批 Controller
 * 普通用户提交恢复/删除申请，管理员审批
 */
@RestController
@RequestMapping("/api/recycle-approval")
public class RecycleApprovalController {

    @Autowired
    private RecycleApprovalService approvalService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserServiceImpl userService;

    /**
     * 提交审批申请（普通用户）
     * POST /api/recycle-approval/submit
     * body: {"bookId": 1, "approvalType": "RESTORE"}
     */
    @PostMapping("/submit")
    @OperationLog(module = "回收站审批", description = "提交审批申请", operationType = "CREATE")
    public Result<RecycleApproval> submit(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser user = getCurrentUser(authHeader);
        if (user == null) {
            return new Result<>(401, null, "请先登录");
        }
        Integer bookId = request.get("bookId") != null ? Integer.valueOf(request.get("bookId").toString()) : null;
        String approvalType = (String) request.get("approvalType");
        if (bookId == null) {
            return new Result<>(400, null, "商品ID不能为空");
        }
        if (approvalType == null || (!"RESTORE".equals(approvalType) && !"DELETE".equals(approvalType))) {
            return new Result<>(400, null, "申请类型无效，RESTORE-恢复 DELETE-彻底删除");
        }
        try {
            RecycleApproval approval = approvalService.submitApproval(bookId, approvalType, user.getId(), user.getUsername());
            return new Result<>(200, approval, "申请已提交，等待管理员审批");
        } catch (Exception e) {
            return new Result<>(500, null, e.getMessage());
        }
    }

    /**
     * 审批通过（管理员）
     * PUT /api/recycle-approval/{id}/approve
     */
    @PutMapping("/{id}/approve")
    @OperationLog(module = "回收站审批", description = "审批通过", operationType = "UPDATE")
    public Result<String> approve(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser user = getAdminUser(authHeader);
        if (user == null) {
            return new Result<>(403, null, "无权限操作，仅管理员可审批");
        }
        String remark = request != null ? request.get("remark") : null;
        try {
            approvalService.approve(id, user.getId(), user.getUsername(), remark);
            return new Result<>(200, null, "审批通过，操作已执行");
        } catch (Exception e) {
            return new Result<>(500, null, e.getMessage());
        }
    }

    /**
     * 审批拒绝（管理员）
     * PUT /api/recycle-approval/{id}/reject
     */
    @PutMapping("/{id}/reject")
    @OperationLog(module = "回收站审批", description = "审批拒绝", operationType = "UPDATE")
    public Result<String> reject(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser user = getAdminUser(authHeader);
        if (user == null) {
            return new Result<>(403, null, "无权限操作，仅管理员可审批");
        }
        String remark = request != null ? request.get("remark") : null;
        try {
            approvalService.reject(id, user.getId(), user.getUsername(), remark);
            return new Result<>(200, null, "已拒绝申请");
        } catch (Exception e) {
            return new Result<>(500, null, e.getMessage());
        }
    }

    /**
     * 查询待审批列表（管理员）
     * GET /api/recycle-approval/pending
     */
    @GetMapping("/pending")
    public Result<List<RecycleApproval>> getPendingList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser user = getAdminUser(authHeader);
        if (user == null) {
            return new Result<>(403, null, "无权限访问");
        }
        List<RecycleApproval> list = approvalService.getPendingList();
        return new Result<>(200, list, "查询成功");
    }

    /**
     * 查询我的申请列表（所有登录用户）
     * GET /api/recycle-approval/my
     */
    @GetMapping("/my")
    public Result<List<RecycleApproval>> getMyApplications(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser user = getCurrentUser(authHeader);
        if (user == null) {
            return new Result<>(401, null, "请先登录");
        }
        List<RecycleApproval> list = approvalService.getMyApplications(user.getId());
        return new Result<>(200, list, "查询成功");
    }

    /**
     * 获取待审批数量（管理员）
     * GET /api/recycle-approval/pending-count
     */
    @GetMapping("/pending-count")
    public Result<Map<String, Object>> getPendingCount(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        SysUser user = getAdminUser(authHeader);
        if (user == null) {
            return new Result<>(403, null, "无权限访问");
        }
        long count = approvalService.countPending();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("count", count);
        return new Result<>(200, data, "查询成功");
    }

    /* ==================== 辅助方法 ==================== */

    private SysUser getCurrentUser(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token == null || !jwtUtil.validateToken(token)) {
                return null;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) return null;
            return userService.getById(userId);
        } catch (Exception e) {
            return null;
        }
    }

    private SysUser getAdminUser(String authHeader) {
        SysUser user = getCurrentUser(authHeader);
        if (user == null || user.getRole() == null || user.getRole() != 1) {
            return null;
        }
        return user;
    }
}
