package com.tiamo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiamo.annotation.OperationLog;
import com.tiamo.entity.Books;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.entity.SysUser;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.BooksService;
import com.tiamo.service.EmailService;
import com.tiamo.service.SysOperationLogService;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

/**
 * 数据导出 Controller
 * 提供用户列表、商品数据、操作日志的CSV下载功能
 * 所有接口路径: /api/export/**
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    @Autowired
    private SysUserServiceImpl userService;

    @Autowired
    private BooksService booksService;

    @Autowired
    private SysOperationLogService operationLogService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private EmailService emailService;

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 通用日期格式化（支持 Date 和 LocalDateTime）
     */
    private String formatDateTime(Object dateTime) {
        if (dateTime == null) {
            return "";
        }
        if (dateTime instanceof Date) {
            return DATE_FORMAT.format((Date) dateTime);
        } else if (dateTime instanceof LocalDateTime) {
            return ((LocalDateTime) dateTime).format(LOCAL_DATE_TIME_FORMATTER);
        }
        return dateTime.toString();
    }

    /**
     * 导出用户列表（仅管理员）
     * GET /api/export/users
     */
    @GetMapping("/users")
    @OperationLog(module = "数据导出", description = "导出用户列表", operationType = "EXPORT")
    public ResponseEntity<byte[]> exportUsers(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        // 验证管理员权限
        SysUser currentUser = getAdminUser(authHeader);
        if (currentUser == null) {
            return ResponseEntity.status(403).body("无权限访问，仅管理员可操作".getBytes(StandardCharsets.UTF_8));
        }

        try {
            List<SysUser> users = userService.list();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

            // 写入BOM头，确保Excel正确识别UTF-8
            writer.write('\uFEFF');
            // CSV表头
            writer.println("ID,用户名,昵称,邮箱,手机号,角色,状态,创建时间");

            for (SysUser user : users) {
                String role = (user.getRole() != null && user.getRole() == 1) ? "管理员" : "普通用户";
                String status = (user.getStatus() != null && user.getStatus() == 1) ? "启用" : "禁用";
                String createTime = formatDateTime(user.getCreateTime());
                writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s",
                        user.getId(),
                        escapeCsv(user.getUsername()),
                        escapeCsv(user.getNickname()),
                        escapeCsv(user.getEmail()),
                        escapeCsv(user.getPhone()),
                        role,
                        status,
                        createTime));
            }
            writer.flush();
            writer.close();

            String filename = "用户列表_" + getDateString() + ".csv";
            return createCsvResponse(baos.toByteArray(), filename);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("导出失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 导出商品数据
     * GET /api/export/books
     */
    @GetMapping("/books")
    @OperationLog(module = "数据导出", description = "导出商品数据", operationType = "EXPORT")
    public ResponseEntity<byte[]> exportBooks() {
        try {
            List<Books> books = booksService.list();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

            writer.write('\uFEFF');
            writer.println("ID,分销软件,微信账号,软件账号,微信备注名,商品ID,商品链接,商品主图,商品标题");

            for (Books book : books) {
                writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
                        book.getId(),
                        escapeCsv(book.getName()),
                        escapeCsv(book.getType()),
                        escapeCsv(book.getDescription()),
                        escapeCsv(book.getAa()),
                        escapeCsv(book.getBd()),
                        escapeCsv(book.getAc()),
                        escapeCsv(book.getAb()),
                        escapeCsv(book.getAx())));
            }
            writer.flush();
            writer.close();

            String filename = "商品数据_" + getDateString() + ".csv";
            return createCsvResponse(baos.toByteArray(), filename);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("导出失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 导出操作日志（仅管理员）
     * GET /api/export/logs
     */
    @GetMapping("/logs")
    @OperationLog(module = "数据导出", description = "导出操作日志", operationType = "EXPORT")
    public ResponseEntity<byte[]> exportLogs(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String operationType,
            @RequestParam(required = false) String username) {
        // 验证管理员权限
        SysUser currentUser = getAdminUser(authHeader);
        if (currentUser == null) {
            return ResponseEntity.status(403).body("无权限访问，仅管理员可操作".getBytes(StandardCharsets.UTF_8));
        }

        try {
            LambdaQueryWrapper<SysOperationLog> wrapper = new LambdaQueryWrapper<>();
            if (module != null && !module.isEmpty()) {
                wrapper.like(SysOperationLog::getModule, module);
            }
            if (operationType != null && !operationType.isEmpty()) {
                wrapper.eq(SysOperationLog::getOperationType, operationType);
            }
            if (username != null && !username.isEmpty()) {
                wrapper.like(SysOperationLog::getUsername, username);
            }
            wrapper.orderByDesc(SysOperationLog::getCreateTime);

            List<SysOperationLog> logs = operationLogService.list(wrapper);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

            writer.write('\uFEFF');
            writer.println("ID,模块,操作描述,操作类型,用户名,IP地址,请求方法,请求URL,状态,耗时(ms),创建时间");

            for (SysOperationLog log : logs) {
                String createTime = formatDateTime(log.getCreateTime());
                String costTime = log.getCostTime() != null ? String.valueOf(log.getCostTime()) : "";
                writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                        log.getId(),
                        escapeCsv(log.getModule()),
                        escapeCsv(log.getDescription()),
                        escapeCsv(log.getOperationType()),
                        escapeCsv(log.getUsername()),
                        escapeCsv(log.getIp()),
                        escapeCsv(log.getMethod()),
                        escapeCsv(log.getUrl()),
                        escapeCsv(log.getStatus()),
                        costTime,
                        createTime));
            }
            writer.flush();
            writer.close();

            String filename = "操作日志_" + getDateString() + ".csv";
            return createCsvResponse(baos.toByteArray(), filename);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("导出失败: " + e.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * 创建CSV下载响应
     */
    private ResponseEntity<byte[]> createCsvResponse(byte[] content, String filename) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        String encodedFilename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
        headers.setContentDispositionFormData("attachment", encodedFilename);
        headers.set("Content-Disposition", "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename);
        return new ResponseEntity<>(content, headers, 200);
    }

    /**
     * CSV字段转义（处理逗号、引号、换行）
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    /**
     * 获取日期字符串（用于文件名）
     */
    private String getDateString() {
        return new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
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

    /* ==================== 导出数据发送到邮箱 ==================== */

    /**
     * 导出用户列表并发送到邮箱（仅管理员）
     * POST /api/export/users/email
     */
    @PostMapping("/users/email")
    @OperationLog(module = "数据导出", description = "导出用户列表到邮箱", operationType = "EXPORT")
    public ResponseEntity<java.util.Map<String, Object>> exportUsersToEmail(
            @RequestBody java.util.Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            SysUser currentUser = getAdminUser(authHeader);
            if (currentUser == null) {
                result.put("code", 403);
                result.put("msg", "无权限访问，仅管理员可操作");
                return ResponseEntity.status(403).body(result);
            }

            String toEmail = request.get("email");
            if (toEmail == null || toEmail.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "邮箱地址不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            List<SysUser> users = userService.list();
            byte[] csvData = generateUsersCsv(users);
            String filename = "用户列表_" + getDateString() + ".csv";

            String htmlContent = "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #6366f1;'>Tiamo AI 数据导出</h2>"
                    + "<p>您好，附件是您请求导出的用户列表数据，共 " + users.size() + " 条记录。</p>"
                    + "<p>导出时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>"
                    + "<p style='color: #94a3b8; font-size: 12px; margin-top: 20px;'>此邮件由系统自动发送，请勿直接回复。</p>"
                    + "</div>";

            emailService.sendEmailWithAttachment(toEmail, "【数据导出】用户列表", htmlContent, csvData, filename);

            result.put("code", 200);
            result.put("msg", "用户列表已发送到邮箱: " + toEmail);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "发送失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 导出商品数据并发送到邮箱
     * POST /api/export/books/email
     */
    @PostMapping("/books/email")
    @OperationLog(module = "数据导出", description = "导出商品数据到邮箱", operationType = "EXPORT")
    public ResponseEntity<java.util.Map<String, Object>> exportBooksToEmail(
            @RequestBody java.util.Map<String, String> request) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            String toEmail = request.get("email");
            if (toEmail == null || toEmail.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "邮箱地址不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            List<Books> books = booksService.list();
            byte[] csvData = generateBooksCsv(books);
            String filename = "商品数据_" + getDateString() + ".csv";

            String htmlContent = "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #6366f1;'>Tiamo AI 数据导出</h2>"
                    + "<p>您好，附件是您请求导出的商品数据，共 " + books.size() + " 条记录。</p>"
                    + "<p>导出时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>"
                    + "<p style='color: #94a3b8; font-size: 12px; margin-top: 20px;'>此邮件由系统自动发送，请勿直接回复。</p>"
                    + "</div>";

            emailService.sendEmailWithAttachment(toEmail, "【数据导出】商品数据", htmlContent, csvData, filename);

            result.put("code", 200);
            result.put("msg", "商品数据已发送到邮箱: " + toEmail);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "发送失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * 导出操作日志并发送到邮箱（仅管理员）
     * POST /api/export/logs/email
     */
    @PostMapping("/logs/email")
    @OperationLog(module = "数据导出", description = "导出操作日志到邮箱", operationType = "EXPORT")
    public ResponseEntity<java.util.Map<String, Object>> exportLogsToEmail(
            @RequestBody java.util.Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        try {
            SysUser currentUser = getAdminUser(authHeader);
            if (currentUser == null) {
                result.put("code", 403);
                result.put("msg", "无权限访问，仅管理员可操作");
                return ResponseEntity.status(403).body(result);
            }

            String toEmail = request.get("email");
            if (toEmail == null || toEmail.isEmpty()) {
                result.put("code", 400);
                result.put("msg", "邮箱地址不能为空");
                return ResponseEntity.badRequest().body(result);
            }

            List<SysOperationLog> logs = operationLogService.list(
                    new LambdaQueryWrapper<SysOperationLog>().orderByDesc(SysOperationLog::getCreateTime));
            byte[] csvData = generateLogsCsv(logs);
            String filename = "操作日志_" + getDateString() + ".csv";

            String htmlContent = "<div style='font-family: sans-serif; max-width: 600px; margin: 0 auto;'>"
                    + "<h2 style='color: #6366f1;'>Tiamo AI 数据导出</h2>"
                    + "<p>您好，附件是您请求导出的操作日志，共 " + logs.size() + " 条记录。</p>"
                    + "<p>导出时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + "</p>"
                    + "<p style='color: #94a3b8; font-size: 12px; margin-top: 20px;'>此邮件由系统自动发送，请勿直接回复。</p>"
                    + "</div>";

            emailService.sendEmailWithAttachment(toEmail, "【数据导出】操作日志", htmlContent, csvData, filename);

            result.put("code", 200);
            result.put("msg", "操作日志已发送到邮箱: " + toEmail);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "发送失败: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /* ==================== CSV生成辅助方法 ==================== */

    private byte[] generateUsersCsv(List<SysUser> users) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        writer.write('\uFEFF');
        writer.println("ID,用户名,昵称,邮箱,手机号,角色,状态,创建时间");
        for (SysUser user : users) {
            String role = (user.getRole() != null && user.getRole() == 1) ? "管理员" : "普通用户";
            String status = (user.getStatus() != null && user.getStatus() == 1) ? "启用" : "禁用";
            writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s",
                    user.getId(), escapeCsv(user.getUsername()), escapeCsv(user.getNickname()),
                    escapeCsv(user.getEmail()), escapeCsv(user.getPhone()), role, status,
                    formatDateTime(user.getCreateTime())));
        }
        writer.flush();
        writer.close();
        return baos.toByteArray();
    }

    private byte[] generateBooksCsv(List<Books> books) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        writer.write('\uFEFF');
        writer.println("ID,分销软件,微信账号,软件账号,微信备注名,商品ID,商品链接,商品主图,商品标题");
        for (Books book : books) {
            writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s",
                    book.getId(), escapeCsv(book.getName()), escapeCsv(book.getType()),
                    escapeCsv(book.getDescription()), escapeCsv(book.getAa()),
                    escapeCsv(book.getBd()), escapeCsv(book.getAc()),
                    escapeCsv(book.getAb()), escapeCsv(book.getAx())));
        }
        writer.flush();
        writer.close();
        return baos.toByteArray();
    }

    private byte[] generateLogsCsv(List<SysOperationLog> logs) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));
        writer.write('\uFEFF');
        writer.println("ID,模块,操作描述,操作类型,用户名,IP地址,请求方法,请求URL,状态,耗时(ms),创建时间");
        for (SysOperationLog log : logs) {
            writer.println(String.format("%d,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s",
                    log.getId(), escapeCsv(log.getModule()), escapeCsv(log.getDescription()),
                    escapeCsv(log.getOperationType()), escapeCsv(log.getUsername()),
                    escapeCsv(log.getIp()), escapeCsv(log.getMethod()), escapeCsv(log.getUrl()),
                    escapeCsv(log.getStatus()), log.getCostTime() != null ? log.getCostTime() : 0,
                    formatDateTime(log.getCreateTime())));
        }
        writer.flush();
        writer.close();
        return baos.toByteArray();
    }
}
