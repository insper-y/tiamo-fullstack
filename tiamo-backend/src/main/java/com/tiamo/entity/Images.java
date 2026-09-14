package com.tiamo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("images")
public class Images implements Serializable {
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

    @TableField("user_id")
    private Long userId;

    @TableField("username")
    private String username;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("thumbnail_path")
    private String thumbnailPath;
}
