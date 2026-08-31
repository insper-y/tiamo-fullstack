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
        // 只查询未删除的数据
        return this.list(new LambdaQueryWrapper<Books>()
                .eq(Books::getDeleted, 0)
                .or().isNull(Books::getDeleted)
                .orderByDesc(Books::getId));
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
        LambdaUpdateWrapper<Books> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Books::getId, id)
                .set(Books::getDeleted, 1)
                .set(Books::getDeletedTime, LocalDateTime.now())
                .set(Books::getDeletedBy, operator != null ? operator : "unknown");
        return this.update(wrapper);
    }

    @Override
    public boolean batchSoftDelete(List<Integer> ids, String operator) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        LambdaUpdateWrapper<Books> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Books::getId, ids)
                .set(Books::getDeleted, 1)
                .set(Books::getDeletedTime, LocalDateTime.now())
                .set(Books::getDeletedBy, operator != null ? operator : "unknown");
        return this.update(wrapper);
    }

    @Override
    public List<Books> listDeleted() {
        return this.list(new LambdaQueryWrapper<Books>()
                .eq(Books::getDeleted, 1)
                .orderByDesc(Books::getDeletedTime));
    }

    @Override
    public boolean restore(Integer id) {
        LambdaUpdateWrapper<Books> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Books::getId, id)
                .set(Books::getDeleted, 0)
                .set(Books::getDeletedTime, null)
                .set(Books::getDeletedBy, null);
        return this.update(wrapper);
    }

    @Override
    public boolean batchRestore(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        LambdaUpdateWrapper<Books> wrapper = new LambdaUpdateWrapper<>();
        wrapper.in(Books::getId, ids)
                .set(Books::getDeleted, 0)
                .set(Books::getDeletedTime, null)
                .set(Books::getDeletedBy, null);
        return this.update(wrapper);
    }

    @Override
    public boolean hardDelete(Integer id) {
        return this.removeById(id);
    }
}
