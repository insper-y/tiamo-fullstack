package com.tiamo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.SysConfig;
import com.tiamo.mapper.SysConfigMapper;
import com.tiamo.service.SysConfigService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getConfigValue(String key, String defaultValue) {
        try {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getConfigKey, key);
            SysConfig config = this.getOne(wrapper);
            if (config != null && config.getConfigValue() != null && !config.getConfigValue().isEmpty()) {
                return config.getConfigValue();
            }
        } catch (Exception e) {
            System.err.println("[SysConfig] getConfigValue失败: " + e.getMessage());
        }
        return defaultValue;
    }

    @Override
    public void setConfigValue(String key, String value, String description) {
        try {
            LambdaQueryWrapper<SysConfig> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysConfig::getConfigKey, key);
            SysConfig config = this.getOne(wrapper);
            if (config != null) {
                config.setConfigValue(value);
                config.setUpdateTime(LocalDateTime.now());
                this.updateById(config);
            } else {
                config = new SysConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                config.setDescription(description);
                config.setCreateTime(LocalDateTime.now());
                config.setUpdateTime(LocalDateTime.now());
                this.save(config);
            }
        } catch (Exception e) {
            System.err.println("[SysConfig] setConfigValue失败: " + e.getMessage());
            throw new RuntimeException("保存配置失败: " + e.getMessage(), e);
        }
    }
}
