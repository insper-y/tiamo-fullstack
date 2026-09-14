package com.tiamo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiamo.common.Result;
import com.tiamo.entity.Images;
import com.tiamo.service.ImagesService;
import com.tiamo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImagesService imagesService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result<List<Images>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        LambdaQueryWrapper<Images> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Images::getCreateTime);
        List<Images> list = imagesService.list(wrapper);
        return new Result<>(200, list, "查询成功");
    }

    @PostMapping("/upload/batch")
    public Result<List<Images>> uploadBatch(
            @RequestParam("files") MultipartFile[] files,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (files == null || files.length == 0) {
            return new Result<>(400, null, "请选择要上传的文件");
        }
        Long userId = null;
        String username = "unknown";
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token != null && jwtUtil.validateToken(token)) {
                userId = jwtUtil.getUserIdFromToken(token);
                username = jwtUtil.getUsernameFromToken(token);
            }
        } catch (Exception ignored) {}

        List<Images> result = imagesService.uploadImages(files, userId, username);
        return new Result<>(200, result, "上传成功");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        boolean flag = imagesService.deleteImage(id);
        if (flag) {
            return new Result<>(200, null, "删除成功");
        }
        return new Result<>(500, null, "删除失败");
    }
}
