package com.tiamo.controller;

import com.tiamo.annotation.OperationLog;
import com.tiamo.common.Code;
import com.tiamo.common.Result;
import com.tiamo.entity.Books;
import com.tiamo.service.BooksService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分销商品数据 Controller
 *
 * 接口路径与前端 login.html 完全对应：
 *   GET    /maven/books        查询全部
 *   GET    /maven/books/{id}   根据ID查询
 *   POST   /maven/books/       新增
 *   PUT    /maven/books/       修改
 *   DELETE /maven/books/{id}   删除
 *
 * 返回码与前端约定一致：
 *   20011 新增成功 / 20010 新增失败
 *   20021 删除成功
 *   20031 修改成功 / 20030 修改失败
 *   20041 查询成功
 */
@RestController
@RequestMapping("/maven/books")
public class BooksController {

    @Autowired
    private BooksService booksService;

    /**
     * 查询全部数据
     */
    @GetMapping
    @OperationLog(module = "商品管理", description = "查询商品列表", operationType = "QUERY")
    public Result<List<Books>> getAll() {
        List<Books> list = booksService.listAll();
        return new Result<>(Code.GET_OK, list, "查询成功");
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/{id}")
    @OperationLog(module = "商品管理", description = "查询商品详情", operationType = "QUERY")
    public Result<Books> getById(@PathVariable Integer id) {
        Books books = booksService.getById(id);
        if (books == null) {
            return new Result<>(Code.GET_ERR, null, "数据不存在");
        }
        return new Result<>(Code.GET_OK, books, "查询成功");
    }

    /**
     * 新增
     */
    @PostMapping
    @OperationLog(module = "商品管理", description = "新增商品", operationType = "CREATE")
    public Result<String> add(@RequestBody Books books) {
        boolean flag = booksService.add(books);
        if (flag) {
            return new Result<>(Code.SAVE_OK, null, "新增成功");
        }
        return new Result<>(Code.SAVE_ERR, null, "新增失败");
    }

    /**
     * 修改
     */
    @PutMapping
    @OperationLog(module = "商品管理", description = "修改商品", operationType = "UPDATE")
    public Result<String> update(@RequestBody Books books) {
        boolean flag = booksService.update(books);
        if (flag) {
            return new Result<>(Code.UPDATE_OK, null, "修改成功");
        }
        return new Result<>(Code.UPDATE_ERR, null, "修改失败");
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    @OperationLog(module = "商品管理", description = "删除商品", operationType = "DELETE")
    public Result<String> delete(@PathVariable Integer id) {
        boolean flag = booksService.delete(id);
        if (flag) {
            return new Result<>(Code.DELETE_OK, null, "删除成功");
        }
        return new Result<>(Code.DELETE_ERR, null, "删除失败");
    }
}
