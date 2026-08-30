package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/**
 * 分销商品数据实体
 * 字段命名与前端 login.html 完全对应
 */
@Data
@TableName("books")
public class Books implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /** 分销软件 */
    @TableField("name")
    private String name;

    /** 微信账号 */
    @TableField("type")
    private String type;

    /** 软件账号 */
    @TableField("description")
    private String description;

    /** 微信备注名 */
    @TableField("aa")
    private String aa;

    /** 商品ID */
    @TableField("bd")
    private String bd;

    /** 商品链接 */
    @TableField("ac")
    private String ac;

    /** 商品主图 */
    @TableField("ab")
    private String ab;

    /** 商品标题 */
    @TableField("ax")
    private String ax;
}
