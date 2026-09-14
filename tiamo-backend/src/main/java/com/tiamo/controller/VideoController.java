package com.tiamo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.tiamo.common.Result;
import com.tiamo.entity.Videos;
import com.tiamo.service.VideosService;
import com.tiamo.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/videos")
public class VideoController {

    @Autowired
    private VideosService videosService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public Result<List<Videos>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        LambdaQueryWrapper<Videos> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(Videos::getCreateTime);
        List<Videos> list = videosService.list(wrapper);
        return new Result<>(200, list, "查询成功");
    }

    @PostMapping("/upload/batch")
    public Result<List<Videos>> uploadBatch(
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

        List<Videos> result = videosService.uploadVideos(files, userId, username);
        return new Result<>(200, result, "上传成功，正在转码处理...");
    }

    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id,
                                  @RequestHeader(value = "Authorization", required = false) String authHeader) {
        boolean flag = videosService.deleteVideo(id);
        if (flag) {
            return new Result<>(200, null, "删除成功");
        }
        return new Result<>(500, null, "删除失败");
    }
}
