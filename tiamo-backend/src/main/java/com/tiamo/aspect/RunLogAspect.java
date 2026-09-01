package com.tiamo.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tiamo.entity.SysRunLog;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysRunLogServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 系统运行日志 AOP 切面
 * 拦截所有 Controller 和 Service 方法调用，记录：
 * 类名、方法名、入参、返回值、耗时、异常堆栈、操作人
 */
@Aspect
@Component
public class RunLogAspect {

    @Autowired
    private SysRunLogServiceImpl runLogService;

    @Autowired
    private JwtUtil jwtUtil;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    /**
     * 拦截 Controller 层方法
     * 排除运行日志相关的Controller，避免无限递归
     */
    @Around("execution(* com.tiamo.controller..*.*(..)) && !execution(* com.tiamo.controller.RunLogController.*(..))")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        return processLog(joinPoint, "CONTROLLER");
    }

    /**
     * 拦截 Service 层方法
     * 排除运行日志相关的Service，避免无限递归
     */
    @Around("execution(* com.tiamo.service..*.*(..)) && !execution(* com.tiamo.service.SysRunLogService.*(..)) && !execution(* com.tiamo.service.impl.SysRunLogServiceImpl.*(..))")
    public Object aroundService(ProceedingJoinPoint joinPoint) throws Throwable {
        return processLog(joinPoint, "SERVICE");
    }

    /**
     * 处理运行日志记录
     */
    private Object processLog(ProceedingJoinPoint joinPoint, String level) throws Throwable {
        long startTime = System.currentTimeMillis();
        SysRunLog log = new SysRunLog();

        try {
            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String className = signature.getDeclaringTypeName();
            String methodName = signature.getName();
            log.setClassName(className);
            log.setMethodName(methodName);
            log.setFullMethod(className + "." + methodName);
            log.setLevel(level);

            // 获取请求信息（仅Controller层）
            if ("CONTROLLER".equals(level)) {
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
            }

            // 记录入参（脱敏，限制大小）
            try {
                Object[] args = joinPoint.getArgs();
                String params = objectMapper.writeValueAsString(
                        Arrays.stream(args)
                                .filter(arg -> !(arg instanceof HttpServletRequest))
                                .toArray()
                );
                // 敏感字段脱敏
                params = params.replaceAll("\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"******\"");
                params = params.replaceAll("\"newPassword\"\\s*:\\s*\"[^\"]*\"", "\"newPassword\":\"******\"");
                params = params.replaceAll("\"confirmPassword\"\\s*:\\s*\"[^\"]*\"", "\"confirmPassword\":\"******\"");
                params = params.replaceAll("\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"******\"");
                log.setParams(params.length() > 2000 ? params.substring(0, 2000) + "..." : params);
            } catch (Exception e) {
                log.setParams("参数解析失败: " + e.getMessage());
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 记录返回值（限制大小）
            try {
                String resultStr = objectMapper.writeValueAsString(result);
                log.setResult(resultStr.length() > 2000 ? resultStr.substring(0, 2000) + "..." : resultStr);
            } catch (Exception e) {
                log.setResult("返回值解析失败: " + e.getMessage());
            }

            // 记录成功/失败（返回错误码也记录为失败）
            long costTime = System.currentTimeMillis() - startTime;
            log.setCostTime(costTime);
            boolean isBusinessError = false;
            if (result instanceof com.tiamo.common.Result) {
                com.tiamo.common.Result<?> res = (com.tiamo.common.Result<?>) result;
                if (res.getCode() != null && res.getCode() != 200) {
                    isBusinessError = true;
                    log.setStatus("FAIL");
                    log.setException(res.getMsg() != null ?
                            (res.getMsg().length() > 500 ? res.getMsg().substring(0, 500) : res.getMsg())
                            : "业务错误，错误码: " + res.getCode());
                }
            }
            if (!isBusinessError) {
                log.setStatus("SUCCESS");
            }
            log.setCreateTime(LocalDateTime.now());

            // 异步保存日志
            runLogService.saveLogAsync(log);

            return result;
        } catch (Throwable e) {
            // 记录失败
            long costTime = System.currentTimeMillis() - startTime;
            log.setCostTime(costTime);
            log.setStatus("FAIL");
            log.setException(e.getMessage() != null ?
                    (e.getMessage().length() > 500 ? e.getMessage().substring(0, 500) : e.getMessage())
                    : "未知错误");
            // 记录异常堆栈
            try {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String stack = sw.toString();
                log.setExceptionStack(stack.length() > 3000 ? stack.substring(0, 3000) + "..." : stack);
            } catch (Exception ex) {
                log.setExceptionStack("堆栈解析失败");
            }
            log.setCreateTime(LocalDateTime.now());
            runLogService.saveLogAsync(log);
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
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
