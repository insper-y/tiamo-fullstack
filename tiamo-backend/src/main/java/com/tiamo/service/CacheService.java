package com.tiamo.service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 缓存服务接口
 * 提供Redis缓存的读取、写入、清除操作
 */
public interface CacheService {

    /**
     * 获取缓存
     */
    <T> T get(String key, Class<T> clazz);

    /**
     * 获取缓存列表
     */
    <T> List<T> getList(String key, Class<T> clazz);

    /**
     * 设置缓存（默认过期时间5分钟）
     */
    void set(String key, Object value);

    /**
     * 设置缓存（指定过期时间）
     */
    void set(String key, Object value, long timeout, TimeUnit unit);

    /**
     * 删除缓存
     */
    void delete(String key);

    /**
     * 按前缀批量删除缓存
     */
    void deleteByPrefix(String prefix);

    /**
     * 清除商品列表缓存
     */
    void clearBooksCache();

    /**
     * 清除用户列表缓存
     */
    void clearUsersCache();

    /**
     * 清除操作日志缓存
     */
    void clearOperationLogCache();

    /**
     * 清除回收站缓存
     */
    void clearRecycleCache();

    /**
     * 清除所有业务缓存
     */
    void clearAllCache();
}
