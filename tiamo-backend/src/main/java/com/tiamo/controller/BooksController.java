package com.tiamo.controller;

import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Code;
import com.tiamo.common.Result;
import com.tiamo.entity.Books;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.BooksService;
import com.tiamo.service.RecycleApprovalService;
import com.tiamo.service.impl.SysUserServiceImpl;
import com.tiamo.entity.RecycleApproval;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 分销商品数据 Controller
 *
 * 接口路径与前端 login.html 完全对应：
 *   GET    /maven/books        查询全部（未删除）
 *   GET    /maven/books/{id}   根据ID查询
 *   POST   /maven/books/       新增
 *   PUT    /maven/books/       修改
 *   DELETE /maven/books/{id}   软删除（移入回收站）
 *
 * 新增接口：
 *   GET    /maven/books/recycle       回收站（仅管理员）
 *   PUT    /maven/books/restore/{id}  恢复（仅管理员）
 *   POST   /maven/books/batch-delete  批量软删除
 *   POST   /maven/books/batch-restore 批量恢复（仅管理员）
 *   DELETE /maven/books/hard/{id}     彻底删除（仅管理员）
 */
@RestController
@RequestMapping("/maven/books")
public class BooksController {

    @Autowired
    private BooksService booksService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserServiceImpl userService;

    @Autowired
    private RecycleApprovalService recycleApprovalService;

    /**
     * 查询全部未删除数据
     */
    @GetMapping
    @OperationLog(module = "商品管理", description = "查询商品列表", operationType = "QUERY")
    public Result<List<Books>> getAll() {
        List<Books> list = booksService.listAll();
        return new Result<>(Code.GET_OK, list, "查询成功");
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    @OperationLog(module = "商品管理", description = "查询商品详情", operationType = "QUERY")
    public Result<Books> getById(@PathVariable Integer id) {
        Books books = booksService.getById(id);
        if (books == null) {
            return new Result<>(Code.GET_ERR, null, "数据不存在");
        }
        return new Result<>(Code.GET_OK, books, "查询成功");
    }

    /**
     * 新增
     */
    @PostMapping
    @OperationLog(module = "商品管理", description = "新增商品", operationType = "CREATE")
    public Result<String> add(@RequestBody Books books) {
        boolean flag = booksService.add(books);
        if (flag) {
            return new Result<>(Code.SAVE_OK, null, "新增成功");
        }
        return new Result<>(Code.SAVE_ERR, null, "新增失败");
    }

    /**
     * 修改
     */
    @PutMapping
    @OperationLog(module = "商品管理", description = "修改商品", operationType = "UPDATE")
    public Result<String> update(@RequestBody Books books) {
        boolean flag = booksService.update(books);
        if (flag) {
            return new Result<>(Code.UPDATE_OK, null, "修改成功");
        }
        return new Result<>(Code.UPDATE_ERR, null, "修改失败");
    }

    /**
     * 软删除（移入回收站，可恢复）
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "商品管理", description = "删除商品（软删除）", operationType = "DELETE")
    public Result<String> delete(@PathVariable Integer id,
                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        String operator = getOperatorFromToken(authHeader);
        boolean flag = booksService.softDelete(id, operator);
        if (flag) {
            return new Result<>(Code.DELETE_OK, null, "删除成功（已移入回收站，可由管理员恢复）");
        }
        return new Result<>(Code.DELETE_ERR, null, "删除失败");
    }

    /**
     * 批量软删除
     */
    @PostMapping("/batch-delete")
    @OperationLog(module = "商品管理", description = "批量删除商品", operationType = "DELETE")
    public Result<String> batchDelete(@RequestBody Map<String, List<Integer>> request,
                                       @RequestHeader(value = "Authorization", required = false) String authHeader) {
        List<Integer> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return new Result<>(Code.DELETE_ERR, null, "请选择要删除的数据");
        }
        String operator = getOperatorFromToken(authHeader);
        boolean flag = booksService.batchSoftDelete(ids, operator);
        if (flag) {
            return new Result<>(Code.DELETE_OK, null, "批量删除成功（共" + ids.size() + "条，已移入回收站）");
        }
        return new Result<>(Code.DELETE_ERR, null, "批量删除失败");
    }

    /**
     * 查询回收站（所有登录用户可查看，普通用户需提交申请才能恢复/删除）
     */
    @GetMapping("/recycle")
    @OperationLog(module = "商品管理", description = "查看回收站", operationType = "QUERY")
    public Result<List<Books>> getRecycleList(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 所有登录用户都可查看回收站
        List<Books> list = booksService.listDeleted();
        
        // 查询当前用户的申请记录，设置申请状态
        Long userId = null;
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                userId = jwtUtil.getUserIdFromToken(token);
            } catch (Exception e) {
                // ignore
            }
        }
        
        if (userId != null && list != null && !list.isEmpty()) {
            List<RecycleApproval> approvals = recycleApprovalService.list(
                new LambdaQueryWrapper<RecycleApproval>()
                    .eq(RecycleApproval::getApplicantId, userId)
            );
            // 构建bookId -> approval的映射
            Map<Integer, RecycleApproval> approvalMap = new java.util.HashMap<>();
            for (RecycleApproval approval : approvals) {
                // 只保留最新的申请记录
                approvalMap.put(approval.getBookId().intValue(), approval);
            }
            // 设置申请状态
            for (Books book : list) {
                RecycleApproval approval = approvalMap.get(book.getId());
                if (approval != null) {
                    book.setApprovalStatus(approval.getStatus());
                    book.setApprovalType(approval.getApprovalType());
                }
            }
        }
        
        return new Result<>(Code.GET_OK, list, "查询成功");
    }

    /**
     * 恢复已删除数据（仅管理员）
     */
    @PutMapping("/restore/{id}")
    @OperationLog(module = "商品管理", description = "恢复商品", operationType = "UPDATE")
    public Result<String> restore(@PathVariable Integer id,
                                   @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作，仅管理员可恢复");
        }
        boolean flag = booksService.restore(id);
        if (flag) {
            return new Result<>(Code.UPDATE_OK, null, "恢复成功");
        }
        return new Result<>(Code.UPDATE_ERR, null, "恢复失败");
    }

    /**
     * 批量恢复（仅管理员）
     */
    @PostMapping("/batch-restore")
    @OperationLog(module = "商品管理", description = "批量恢复商品", operationType = "UPDATE")
    public Result<String> batchRestore(@RequestBody Map<String, List<Integer>> request,
                                        @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作，仅管理员可恢复");
        }
        List<Integer> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return new Result<>(Code.UPDATE_ERR, null, "请选择要恢复的数据");
        }
        boolean flag = booksService.batchRestore(ids);
        if (flag) {
            return new Result<>(Code.UPDATE_OK, null, "批量恢复成功（共" + ids.size() + "条）");
        }
        return new Result<>(Code.UPDATE_ERR, null, "批量恢复失败");
    }

    /**
     * 彻底删除（从数据库移除，不可恢复，仅管理员）
     */
    @DeleteMapping("/hard/{id}")
    @OperationLog(module = "商品管理", description = "彻底删除商品", operationType = "DELETE")
    public Result<String> hardDelete(@PathVariable Integer id,
                                      @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作，仅管理员可彻底删除");
        }
        boolean flag = booksService.hardDelete(id);
        if (flag) {
            return new Result<>(Code.DELETE_OK, null, "彻底删除成功（不可恢复）");
        }
        return new Result<>(Code.DELETE_ERR, null, "彻底删除失败");
    }

    /**
     * 批量彻底删除（从数据库物理移除，不可恢复，仅管理员）
     */
    @PostMapping("/hard/batch")
    @OperationLog(module = "商品管理", description = "批量彻底删除商品", operationType = "DELETE")
    public Result<String> batchHardDelete(@RequestBody Map<String, List<Integer>> request,
                                           @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (!isAdmin(authHeader)) {
            return new Result<>(403, null, "无权限操作，仅管理员可批量彻底删除");
        }
        List<Integer> ids = request.get("ids");
        if (ids == null || ids.isEmpty()) {
            return new Result<>(Code.DELETE_ERR, null, "请选择要彻底删除的数据");
        }
        boolean flag = booksService.batchHardDelete(ids);
        if (flag) {
            return new Result<>(Code.DELETE_OK, null, "批量彻底删除成功（共" + ids.size() + "条，不可恢复）");
        }
        return new Result<>(Code.DELETE_ERR, null, "批量彻底删除失败");
    }

    /* ==================== 辅助方法 ==================== */

    /**
     * 从Token中获取操作人用户名
     */
    private String getOperatorFromToken(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtUtil.validateToken(token)) {
                return jwtUtil.getUsernameFromToken(token);
            }
        } catch (Exception ignored) {
        }
        return "unknown";
    }

    /**
     * 验证当前用户是否为管理员
     */
    private boolean isAdmin(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token == null || !jwtUtil.validateToken(token)) {
                return false;
            }
            Long userId = jwtUtil.getUserIdFromToken(token);
            if (userId == null) {
                return false;
            }
            SysUser user = userService.getById(userId);
            return user != null && user.getRole() != null && user.getRole() == 1;
        } catch (Exception e) {
            return false;
        }
    }
}
