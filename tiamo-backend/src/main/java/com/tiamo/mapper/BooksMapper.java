package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.Books;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 分销商品数据 Mapper
 */
@Mapper
public interface BooksMapper extends BaseMapper<Books> {

    /**
     * 添加软删除标记列（MySQL不支持IF NOT EXISTS，调用方需捕获重复列异常）
     */
    @Update("ALTER TABLE books ADD COLUMN deleted INT DEFAULT 0 COMMENT '软删除标记 0-未删除 1-已删除'")
    void addDeletedColumn();

    @Update("ALTER TABLE books ADD COLUMN deleted_time DATETIME NULL COMMENT '删除时间'")
    void addDeletedTimeColumn();

    @Update("ALTER TABLE books ADD COLUMN deleted_by VARCHAR(100) NULL COMMENT '删除操作人'")
    void addDeletedByColumn();
}
