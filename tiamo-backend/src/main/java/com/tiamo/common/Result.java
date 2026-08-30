package com.tiamo.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一返回结果
 * 格式与前端约定一致：{ code, data, msg }
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 状态码 */
    private Integer code;

    /** 数据 */
    private T data;

    /** 提示信息 */
    private String msg;

    public Result() {
    }

    public Result(Integer code, T data, String msg) {
        this.code = code;
        this.data = data;
        this.msg = msg;
    }

    public Result(Integer code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    /** 成功返回（带数据） */
    public static <T> Result<T> success(T data) {
        return new Result<>(Code.GET_OK, data, "操作成功");
    }

    /** 成功返回（无数据） */
    public static <T> Result<T> success() {
        return new Result<>(Code.GET_OK, null, "操作成功");
    }

    /** 失败返回 */
    public static <T> Result<T> error(String msg) {
        return new Result<>(Code.SYSTEM_ERR, null, msg);
    }
}
