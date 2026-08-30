package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.Books;

import java.util.List;

/**
 * 分销商品数据 Service 接口
 */
public interface BooksService extends IService<Books> {

    /**
     * 查询所有数据
     */
    List<Books> listAll();

    /**
     * 根据ID查询
     */
    Books getById(Integer id);

    /**
     * 新增
     */
    boolean add(Books books);

    /**
     * 修改
     */
    boolean update(Books books);

    /**
     * 删除
     */
    boolean delete(Integer id);
}
