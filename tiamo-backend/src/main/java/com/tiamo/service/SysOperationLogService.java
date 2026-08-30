package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.SysOperationLog;

/**
 * 操作日志 Service 接口
 */
public interface SysOperationLogService extends IService<SysOperationLog> {

    /**
     * 异步保存操作日志
     */
    void saveLogAsync(SysOperationLog log);
}
