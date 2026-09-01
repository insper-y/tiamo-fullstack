package com.tiamo.controller;

import com.tiamo.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务器状态监控 Controller
 * 提供CPU、内存、JVM、磁盘、线程、运行时长等系统信息
 */
@RestController
@RequestMapping("/api/server-status")
public class ServerStatusController {

    @GetMapping
    public Result<Map<String, Object>> getServerStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            // CPU信息
            OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
            double systemCpuLoad = getDoubleFromBean(osBean, "getSystemCpuLoad", 0.0) * 100;
            double processCpuLoad = getDoubleFromBean(osBean, "getProcessCpuLoad", 0.0) * 100;
            int availableProcessors = osBean.getAvailableProcessors();

            Map<String, Object> cpu = new HashMap<>();
            cpu.put("systemCpuLoad", Math.round(systemCpuLoad * 100.0) / 100.0);
            cpu.put("processCpuLoad", Math.round(processCpuLoad * 100.0) / 100.0);
            cpu.put("cores", availableProcessors);
            status.put("cpu", cpu);

            // 系统内存信息
            long totalMemory = getLongFromBean(osBean, "getTotalPhysicalMemorySize", 0L);
            long freeMemory = getLongFromBean(osBean, "getFreePhysicalMemorySize", 0L);
            long usedMemory = totalMemory > 0 ? totalMemory - freeMemory : 0L;
            double memoryUsage = totalMemory > 0 ? (double) usedMemory / totalMemory * 100 : 0.0;

            Map<String, Object> memory = new HashMap<>();
            memory.put("total", formatBytes(totalMemory));
            memory.put("used", formatBytes(usedMemory));
            memory.put("free", formatBytes(freeMemory));
            memory.put("usagePercent", Math.round(memoryUsage * 100.0) / 100.0);
            memory.put("totalBytes", totalMemory);
            memory.put("usedBytes", usedMemory);
            memory.put("freeBytes", freeMemory);
            status.put("memory", memory);

            // JVM内存信息
            Runtime runtime = Runtime.getRuntime();
            long jvmTotal = runtime.totalMemory();
            long jvmFree = runtime.freeMemory();
            long jvmUsed = jvmTotal - jvmFree;
            long jvmMax = runtime.maxMemory();
            double jvmUsage = jvmMax > 0 ? (double) jvmUsed / jvmMax * 100 : 0.0;

            Map<String, Object> jvm = new HashMap<>();
            jvm.put("total", formatBytes(jvmTotal));
            jvm.put("used", formatBytes(jvmUsed));
            jvm.put("free", formatBytes(jvmFree));
            jvm.put("max", formatBytes(jvmMax));
            jvm.put("usagePercent", Math.round(jvmUsage * 100.0) / 100.0);
            jvm.put("totalBytes", jvmTotal);
            jvm.put("usedBytes", jvmUsed);
            jvm.put("freeBytes", jvmFree);
            jvm.put("maxBytes", jvmMax);
            status.put("jvm", jvm);

            // 磁盘信息
            File root = new File("/");
            long diskTotal = root.getTotalSpace();
            long diskFree = root.getFreeSpace();
            long diskUsed = diskTotal > 0 ? diskTotal - diskFree : 0L;
            double diskUsage = diskTotal > 0 ? (double) diskUsed / diskTotal * 100 : 0.0;

            Map<String, Object> disk = new HashMap<>();
            disk.put("total", formatBytes(diskTotal));
            disk.put("used", formatBytes(diskUsed));
            disk.put("free", formatBytes(diskFree));
            disk.put("usagePercent", Math.round(diskUsage * 100.0) / 100.0);
            disk.put("totalBytes", diskTotal);
            disk.put("usedBytes", diskUsed);
            disk.put("freeBytes", diskFree);
            status.put("disk", disk);

            // 线程信息
            ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
            int threadCount = threadBean.getThreadCount();
            int peakThreadCount = threadBean.getPeakThreadCount();
            int daemonThreadCount = threadBean.getDaemonThreadCount();

            Map<String, Object> threads = new HashMap<>();
            threads.put("current", threadCount);
            threads.put("peak", peakThreadCount);
            threads.put("daemon", daemonThreadCount);
            status.put("threads", threads);

            // 运行时长
            RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
            long uptime = runtimeBean.getUptime();
            String uptimeFormatted = formatUptime(uptime);

            Map<String, Object> runtimeInfo = new HashMap<>();
            runtimeInfo.put("uptimeMs", uptime);
            runtimeInfo.put("uptimeFormatted", uptimeFormatted);
            runtimeInfo.put("startTime", runtimeBean.getStartTime());
            runtimeInfo.put("vmName", runtimeBean.getVmName());
            runtimeInfo.put("vmVersion", runtimeBean.getVmVersion());
            status.put("runtime", runtimeInfo);

            // 系统信息
            Map<String, Object> system = new HashMap<>();
            system.put("osName", osBean.getName());
            system.put("osVersion", osBean.getVersion());
            system.put("osArch", osBean.getArch());
            system.put("javaVersion", System.getProperty("java.version"));
            status.put("system", system);

            return new Result<>(200, status, "获取成功");
        } catch (Exception e) {
            e.printStackTrace();
            return new Result<>(500, null, "获取服务器状态失败: " + e.getMessage());
        }
    }

    /**
     * 通过反射调用OperatingSystemMXBean的double方法
     */
    private double getDoubleFromBean(OperatingSystemMXBean bean, String methodName, double defaultValue) {
        try {
            Object result = bean.getClass().getMethod(methodName).invoke(bean);
            if (result instanceof Double) {
                double value = (Double) result;
                return value >= 0 ? value : defaultValue;
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 通过反射调用OperatingSystemMXBean的long方法
     */
    private long getLongFromBean(OperatingSystemMXBean bean, String methodName, long defaultValue) {
        try {
            Object result = bean.getClass().getMethod(methodName).invoke(bean);
            if (result instanceof Long) {
                return (Long) result;
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * 格式化字节数
     */
    private String formatBytes(long bytes) {
        if (bytes <= 0) return "0 B";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }

    /**
     * 格式化运行时长
     */
    private String formatUptime(long uptimeMs) {
        long seconds = uptimeMs / 1000;
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("天 ");
        if (hours > 0) sb.append(hours).append("小时 ");
        if (minutes > 0) sb.append(minutes).append("分 ");
        sb.append(secs).append("秒");
        return sb.toString();
    }
}
