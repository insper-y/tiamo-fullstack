package com.tiamo.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiamo.annotation.OperationLog;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysOperationLogServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 操作日志 AOP 切面
 * 拦截所有标注 @OperationLog 的方法，自动记录操作日志并发送邮件
 */
@Aspect
@Component
public class OperationLogAspect {

    @Autowired
    private SysOperationLogServiceImpl operationLogService;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private com.tiamo.service.EmailService emailService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operationLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperationLog operationLog) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysOperationLog log = new SysOperationLog();

        try {
            // 获取请求信息
            HttpServletRequest request = getRequest();
            if (request != null) {
                log.setIp(getClientIp(request));
                log.setUrl(request.getRequestURI());

                // 从 Token 解析用户信息
                String token = jwtUtil.extractTokenFromHeader(request.getHeader("Authorization"));
                if (token != null && jwtUtil.validateToken(token)) {
                    log.setUserId(jwtUtil.getUserIdFromToken(token));
                    log.setUsername(jwtUtil.getUsernameFromToken(token));
                }
            }

            // 方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            log.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());

            // 注解信息
            log.setModule(operationLog.module());
            log.setDescription(operationLog.description());
            log.setOperationType(operationLog.operationType());

            // 请求参数（脱敏：密码字段不记录）
            try {
                Object[] args = joinPoint.getArgs();
                String params = objectMapper.writeValueAsString(
                        Arrays.stream(args)
                                .filter(arg -> !(arg instanceof HttpServletRequest))
                                .toArray()
                );
                // 简单脱敏
                params = params.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"******\"");
                params = params.replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]*\"", "\"newPassword\":\"******\"");
                params = params.replaceAll("\"confirmPassword\"\\s*:\\s*\"[^\"]*\"", "\"confirmPassword\":\"******\"");
                log.setParams(params.length() > 2000 ? params.substring(0, 2000) + "..." : params);
            } catch (Exception e) {
                log.setParams("参数解析失败");
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录成功
            long costTime = System.currentTimeMillis() - startTime;
            log.setCostTime(costTime);
            log.setStatus("SUCCESS");
            log.setCreateTime(LocalDateTime.now());

            // 异步保存日志
            operationLogService.saveLogAsync(log);

            // 实时发送邮件
            emailService.sendOperationLogEmailAsync(log);

            return result;

        } catch (Throwable e) {
            // 记录失败
            long costTime = System.currentTimeMillis() - startTime;
            log.setCostTime(costTime);
            log.setStatus("FAIL");
            log.setErrorMsg(e.getMessage() != null ?
                    (e.getMessage().length() > 500 ? e.getMessage().substring(0, 500) : e.getMessage())
                    : "未知错误");
            log.setCreateTime(LocalDateTime.now());

            operationLogService.saveLogAsync(log);
            emailService.sendOperationLogEmailAsync(log);

            throw e;
        }
    }

    private HttpServletRequest getRequest() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attributes != null ? attributes.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多级代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
