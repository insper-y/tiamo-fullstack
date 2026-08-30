package com.tiamo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.SysOperationLog;
import com.tiamo.mapper.SysOperationLogMapper;
import com.tiamo.service.SysOperationLogService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * 操作日志 Service 实现
 */
@Service
public class SysOperationLogServiceImpl
        extends ServiceImpl<SysOperationLogMapper, SysOperationLog>
        implements SysOperationLogService {

    @Override
    @Async("logExecutor")
    public void saveLogAsync(SysOperationLog log) {
        this.save(log);
    }
}
