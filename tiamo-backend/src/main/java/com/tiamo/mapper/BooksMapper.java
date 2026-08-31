package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.Books;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

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

    /**
     * 查询已删除的数据（回收站）- 原生SQL绕过逻辑删除过滤
     */
    @Select("SELECT * FROM books WHERE deleted = 1 ORDER BY deleted_time DESC")
    List<Books> selectDeletedList();

    /**
     * 恢复已删除的数据 - 原生SQL绕过逻辑删除过滤
     */
    @Update("UPDATE books SET deleted = 0, deleted_time = NULL, deleted_by = NULL WHERE id = #{id}")
    int restoreById(@Param("id") Integer id);

    /**
     * 批量恢复已删除的数据 - 原生SQL绕过逻辑删除过滤
     */
    @Update({
            "<script>",
            "UPDATE books SET deleted = 0, deleted_time = NULL, deleted_by = NULL WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "</script>"
    })
    int batchRestoreByIds(@Param("ids") List<Integer> ids);
}
