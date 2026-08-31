package com.tiamo.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tiamo.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务实现类
 * 基于Redis实现缓存的读取、写入、清除操作
 */
@Service
public class CacheServiceImpl implements CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheServiceImpl.class);

    // 缓存key前缀
    public static final String CACHE_PREFIX = "tiamo:";
    public static final String BOOKS_KEY = CACHE_PREFIX + "books:list";
    public static final String USERS_KEY = CACHE_PREFIX + "users:list";
    public static final String OPERATION_LOG_KEY = CACHE_PREFIX + "operation_log:list";
    public static final String RECYCLE_KEY = CACHE_PREFIX + "recycle:list";

    // 默认缓存过期时间：5分钟
    private static final long DEFAULT_TIMEOUT = 5;
    private static final TimeUnit DEFAULT_UNIT = TimeUnit.MINUTES;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return objectMapper.convertValue(value, clazz);
        } catch (Exception e) {
            log.warn("获取缓存失败 key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public <T> List<T> getList(String key, Class<T> clazz) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return null;
            }
            return objectMapper.convertValue(value, new TypeReference<List<T>>() {});
        } catch (Exception e) {
            log.warn("获取缓存列表失败 key={}, error={}", key, e.getMessage());
            return null;
        }
    }

    @Override
    public void set(String key, Object value) {
        set(key, value, DEFAULT_TIMEOUT, DEFAULT_UNIT);
    }

    @Override
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.warn("设置缓存失败 key={}, error={}", key, e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("删除缓存失败 key={}, error={}", key, e.getMessage());
        }
    }

    @Override
    public void deleteByPrefix(String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
            }
        } catch (Exception e) {
            log.warn("批量删除缓存失败 prefix={}, error={}", prefix, e.getMessage());
        }
    }

    @Override
    public void clearBooksCache() {
        deleteByPrefix(CACHE_PREFIX + "books:");
        log.info("已清除商品列表缓存");
    }

    @Override
    public void clearUsersCache() {
        deleteByPrefix(CACHE_PREFIX + "users:");
        log.info("已清除用户列表缓存");
    }

    @Override
    public void clearOperationLogCache() {
        deleteByPrefix(CACHE_PREFIX + "operation_log:");
        log.info("已清除操作日志缓存");
    }

    @Override
    public void clearRecycleCache() {
        deleteByPrefix(CACHE_PREFIX + "recycle:");
        log.info("已清除回收站缓存");
    }

    @Override
    public void clearAllCache() {
        deleteByPrefix(CACHE_PREFIX);
        log.info("已清除所有业务缓存");
    }
}
