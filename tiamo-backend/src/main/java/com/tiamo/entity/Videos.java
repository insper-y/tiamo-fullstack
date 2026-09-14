package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("videos")
public class Videos implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("file_name")
    private String fileName;

    @TableField("original_name")
    private String originalName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_size")
    private Long fileSize;

    @TableField("mime_type")
    private String mimeType;

    @TableField("duration")
    private Long duration;

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("cover_path")
    private String coverPath;

    @TableField("status")
    private Integer status;

    @TableField("original_path")
    private String originalPath;
}
