package com.tiamo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.Books;
import com.tiamo.mapper.BooksMapper;
import com.tiamo.service.BooksService;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 分销商品数据 Service 实现
 */
@Service
public class BooksServiceImpl extends ServiceImpl<BooksMapper, Books> implements BooksService {

    /**
     * 启动时确保软删除相关列存在
     */
    @PostConstruct
    public void ensureColumns() {
        try {
            baseMapper.addDeletedColumn();
            System.out.println("[Books] 已添加 deleted 列");
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
        // 优化：使用原生SQL只查询需要的字段，走覆盖索引 idx_deleted_id
        return baseMapper.selectAllOptimized();
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
        return this.save(books);
    }

    @Override
    public boolean update(Books books) {
        return this.updateById(books);
    }

    @Override
    public boolean softDelete(Integer id, String operator) {
        // 用原生SQL绕过MyBatis-Plus逻辑删除插件拦截
        return baseMapper.softDeleteById(id, LocalDateTime.now(), operator != null ? operator : "unknown") > 0;
    }

    @Override
    public boolean batchSoftDelete(List<Integer> ids, String operator) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 用原生SQL绕过MyBatis-Plus逻辑删除插件拦截
        return baseMapper.batchSoftDeleteByIds(ids, LocalDateTime.now(), operator != null ? operator : "unknown") > 0;
    }

    @Override
    public List<Books> listDeleted() {
        // 用原生SQL绕过MyBatis-Plus逻辑删除过滤
        return baseMapper.selectDeletedList();
    }

    @Override
    public boolean restore(Integer id) {
        // 用原生SQL绕过MyBatis-Plus逻辑删除过滤
        return baseMapper.restoreById(id) > 0;
    }

    @Override
    public boolean batchRestore(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        // 用原生SQL绕过MyBatis-Plus逻辑删除过滤
        return baseMapper.batchRestoreByIds(ids) > 0;
    }

    @Override
    public boolean hardDelete(Integer id) {
        return this.removeById(id);
    }
}
