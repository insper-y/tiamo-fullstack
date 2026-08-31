package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.Books;

import java.util.List;

/**
 * 分销商品数据 Service 接口
 */
public interface BooksService extends IService<Books> {

    /**
     * 查询所有未删除数据
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
     * 软删除（移入回收站）
     */
    boolean softDelete(Integer id, String operator);

    /**
     * 批量软删除
     */
    boolean batchSoftDelete(List<Integer> ids, String operator);

    /**
     * 查询回收站（已删除数据）
     */
    List<Books> listDeleted();

    /**
     * 恢复已删除数据
     */
    boolean restore(Integer id);

    /**
     * 批量恢复
     */
    boolean batchRestore(List<Integer> ids);

    /**
     * 彻底删除（从数据库移除）
     */
    boolean hardDelete(Integer id);
}
