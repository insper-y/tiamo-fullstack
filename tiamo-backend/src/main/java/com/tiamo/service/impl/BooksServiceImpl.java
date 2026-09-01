package com.tiamo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.Books;
import com.tiamo.mapper.BooksMapper;
import com.tiamo.service.BooksService;
import com.tiamo.service.CacheService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分销商品数据 Service 实现
 * 加入Redis缓存，查询优先走缓存，数据变更时清除缓存
 */
@Service
public class BooksServiceImpl extends ServiceImpl<BooksMapper, Books> implements BooksService {

    private static final Logger log = LoggerFactory.getLogger(BooksServiceImpl.class);

    @Autowired
    private CacheService cacheService;

    /**
     * 启动时确保软删除相关列存在
     */
    @PostConstruct
    public void ensureColumns() {
        try {
            baseMapper.addDeletedColumn();
            log.info("[Books] 已添加 deleted 列");
        } catch (Exception e) {
            // 列已存在，忽略
        }
        try {
            baseMapper.addDeletedTimeColumn();
        } catch (Exception e) {
            // 列已存在，忽略
        }
        try {
            baseMapper.addDeletedByColumn();
        } catch (Exception e) {
            // 列已存在，忽略
        }
    }

    @Override
    public List<Books> listAll() {
        // 先查缓存
        String cacheKey = CacheServiceImpl.BOOKS_KEY;
        List<Books> cachedList = cacheService.getList(cacheKey, Books.class);
        if (cachedList != null) {
            log.debug("从缓存获取商品列表，数量: {}", cachedList.size());
            return cachedList;
        }
        // 缓存未命中，查数据库
        log.debug("缓存未命中，从数据库查询商品列表");
        List<Books> list = baseMapper.selectAllOptimized();
        // 写入缓存（5分钟过期）
        cacheService.set(cacheKey, list);
        return list;
    }

    @Override
    public Books getById(Integer id) {
        return super.getById(id);
    }

    @Override
    public boolean add(Books books) {
        if (books.getDeleted() == null) {
            books.setDeleted(0);
        }
        boolean result = this.save(books);
        if (result) {
            cacheService.clearBooksCache();
            log.info("新增商品成功，已清除商品缓存");
        }
        return result;
    }

    @Override
    public boolean update(Books books) {
        boolean result = this.updateById(books);
        if (result) {
            cacheService.clearBooksCache();
            log.info("修改商品成功，已清除商品缓存");
        }
        return result;
    }

    @Override
    public boolean softDelete(Integer id, String operator) {
        // 用原生SQL绕过MyBatis-Plus逻辑删除插件拦截
        boolean result = baseMapper.softDeleteById(id, LocalDateTime.now(), operator != null ? operator : "unknown") > 0;
        if (result) {
            cacheService.clearBooksCache();
            cacheService.clearRecycleCache();
            log.info("软删除商品成功，已清除商品和回收站缓存");
        }
        return result;
    }

    @Override
    public boolean batchSoftDelete(List<Integer> ids, String operator) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 用原生SQL绕过MyBatis-Plus逻辑删除插件拦截
        boolean result = baseMapper.batchSoftDeleteByIds(ids, LocalDateTime.now(), operator != null ? operator : "unknown") > 0;
        if (result) {
            cacheService.clearBooksCache();
            cacheService.clearRecycleCache();
            log.info("批量软删除商品成功，已清除商品和回收站缓存");
        }
        return result;
    }

    @Override
    public List<Books> listDeleted() {
        // 先查缓存
        String cacheKey = CacheServiceImpl.RECYCLE_KEY;
        List<Books> cachedList = cacheService.getList(cacheKey, Books.class);
        if (cachedList != null) {
            log.debug("从缓存获取回收站列表，数量: {}", cachedList.size());
            return cachedList;
        }
        // 缓存未命中，查数据库
        log.debug("缓存未命中，从数据库查询回收站列表");
        // 用原生SQL绕过MyBatis-Plus逻辑删除过滤
        List<Books> list = baseMapper.selectDeletedList();
        // 写入缓存（5分钟过期）
        cacheService.set(cacheKey, list);
        return list;
    }

    @Override
    public boolean restore(Integer id) {
        // 用原生SQL绕过MyBatis-Plus逻辑删除过滤
        boolean result = baseMapper.restoreById(id) > 0;
        if (result) {
            cacheService.clearBooksCache();
            cacheService.clearRecycleCache();
            log.info("恢复商品成功，已清除商品和回收站缓存");
        }
        return result;
    }

    @Override
    public boolean batchRestore(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 用原生SQL绕过MyBatis-Plus逻辑删除过滤
        boolean result = baseMapper.batchRestoreByIds(ids) > 0;
        if (result) {
            cacheService.clearBooksCache();
            cacheService.clearRecycleCache();
            log.info("批量恢复商品成功，已清除商品和回收站缓存");
        }
        return result;
    }

    @Override
    public boolean hardDelete(Integer id) {
        boolean result = this.removeById(id);
        if (result) {
            cacheService.clearBooksCache();
            cacheService.clearRecycleCache();
            log.info("彻底删除商品成功，已清除商品和回收站缓存");
        }
        return result;
    }

    @Override
    public boolean batchHardDelete(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        boolean result = baseMapper.batchHardDeleteByIds(ids) > 0;
        if (result) {
            cacheService.clearBooksCache();
            cacheService.clearRecycleCache();
            log.info("批量彻底删除商品成功，数量: {}，已清除商品和回收站缓存", ids.size());
        }
        return result;
    }
}
