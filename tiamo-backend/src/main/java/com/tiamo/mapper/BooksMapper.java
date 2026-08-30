package com.tiamo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tiamo.entity.Books;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分销商品数据 Mapper
 */
@Mapper
public interface BooksMapper extends BaseMapper<Books> {
}
