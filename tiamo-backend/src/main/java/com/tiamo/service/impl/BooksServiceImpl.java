package com.tiamo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.Books;
import com.tiamo.mapper.BooksMapper;
import com.tiamo.service.BooksService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分销商品数据 Service 实现
 */
@Service
public class BooksServiceImpl extends ServiceImpl<BooksMapper, Books> implements BooksService {

    @Override
    public List<Books> listAll() {
        return this.list(new LambdaQueryWrapper<Books>().orderByDesc(Books::getId));
    }

    @Override
    public Books getById(Integer id) {
        return super.getById(id);
    }

    @Override
    public boolean add(Books books) {
        return this.save(books);
    }

    @Override
    public boolean update(Books books) {
        return this.updateById(books);
    }

    @Override
    public boolean delete(Integer id) {
        return this.removeById(id);
    }
}
