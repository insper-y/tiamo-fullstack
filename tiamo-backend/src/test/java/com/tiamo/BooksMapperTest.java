package com.tiamo;

import com.tiamo.entity.Books;
import com.tiamo.mapper.BooksMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * 数据访问层测试
 * 运行前确保 MySQL 已启动且 tiamo_db 数据库已初始化
 */
@SpringBootTest
class BooksMapperTest {

    @Autowired
    private BooksMapper booksMapper;

    @Test
    void testSelectAll() {
        List<Books> list = booksMapper.selectList(null);
        System.out.println("查询到 " + list.size() + " 条数据");
        list.forEach(System.out::println);
    }

    @Test
    void testInsert() {
        Books books = new Books();
        books.setName("测试分销软件");
        books.setType("test_wx");
        books.setDescription("test_soft_acc");
        books.setAa("测试备注");
        books.setBd("99999");
        books.setAc("https://test.com/product/99999");
        books.setAb("https://test.com/img/99999.jpg");
        books.setAx("测试商品标题");
        int rows = booksMapper.insert(books);
        System.out.println("插入 " + rows + " 条数据, 自增ID=" + books.getId());
    }
}
