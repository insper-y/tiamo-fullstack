package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.SysConfig;

public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据key获取配置值
     */
    String getConfigValue(String key, String defaultValue);

    /**
     * 设置配置值
     */
    void setConfigValue(String key, String value, String description);
}
