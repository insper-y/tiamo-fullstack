package com.tiamo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tiamo.common.Result;
import com.tiamo.entity.Images;
import com.tiamo.entity.SysUser;
import com.tiamo.entity.Videos;
import com.tiamo.mapper.ImagesMapper;
import com.tiamo.mapper.VideosMapper;
import com.tiamo.security.JwtUtil;
import com.tiamo.service.impl.SysUserServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api")
public class AlbumController {

    @Autowired
    private ImagesMapper imagesMapper;

    @Autowired
    private VideosMapper videosMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private SysUserServiceImpl userService;

    @Value("${upload.path:/tmp/tiamo-uploads}")
    private String uploadPath;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ==================== 图片接口 ====================

    @GetMapping("/images")
    public Result<Map<String, Object>> getImages(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserId(authHeader);
        LambdaQueryWrapper<Images> wrapper = new LambdaQueryWrapper<>();
        if (userId != null && !isAdmin(authHeader)) {
            wrapper.eq(Images::getUserId, userId);
        }
        wrapper.orderByDesc(Images::getId);
        Page<Images> pageResult = imagesMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("pages", pageResult.getPages());
        data.put("current", pageResult.getCurrent());
        data.put("size", pageResult.getSize());
        return new Result<>(200, data, "查询成功");
    }

    @PostMapping("/images/upload")
    public Result<List<Images>> uploadImages(
            @RequestParam("files") MultipartFile[] files,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (files == null || files.length == 0) {
            return new Result<>(400, null, "请选择文件");
        }
        Long userId = getUserId(authHeader);
        String username = getUsername(authHeader);
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dirPath = uploadPath + "/images/" + dateDir;
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        List<Images> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String originalName = file.getOriginalFilename();
                String ext = originalName != null && originalName.contains(".")
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String fileName = DTF.format(LocalDateTime.now()) + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
                String filePath = dirPath + "/" + fileName;
                file.transferTo(new File(filePath));

                Images img = new Images();
                img.setFileName(fileName);
                img.setOriginalName(originalName);
                img.setFilePath("/uploads/images/" + dateDir + "/" + fileName);
                img.setThumbnailPath("/uploads/images/" + dateDir + "/" + fileName);
                img.setFileSize(file.getSize());
                img.setMimeType(file.getContentType());
                img.setUserId(userId);
                img.setUsername(username);
                img.setCreateTime(LocalDateTime.now());
                imagesMapper.insert(img);
                uploaded.add(img);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Result<>(200, uploaded, "上传成功，共" + uploaded.size() + "个文件");
    }

    @DeleteMapping("/images/{id}")
    public Result<String> deleteImage(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Images img = imagesMapper.selectById(id);
        if (img == null) return new Result<>(404, null, "图片不存在");
        Long userId = getUserId(authHeader);
        if (!isAdmin(authHeader) && (userId == null || !userId.equals(img.getUserId()))) {
            return new Result<>(403, null, "无权限删除");
        }
        // 删除物理文件
        try {
            String fullPath = uploadPath + img.getFilePath().replace("/uploads", "");
            File f = new File(fullPath);
            if (f.exists()) f.delete();
        } catch (Exception e) { e.printStackTrace(); }
        imagesMapper.deleteById(id);
        return new Result<>(200, null, "删除成功");
    }

    // ==================== 视频接口 ====================

    @GetMapping("/videos")
    public Result<Map<String, Object>> getVideos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Long userId = getUserId(authHeader);
        LambdaQueryWrapper<Videos> wrapper = new LambdaQueryWrapper<>();
        if (userId != null && !isAdmin(authHeader)) {
            wrapper.eq(Videos::getUserId, userId);
        }
        wrapper.orderByDesc(Videos::getId);
        Page<Videos> pageResult = videosMapper.selectPage(new Page<>(page, size), wrapper);
        Map<String, Object> data = new HashMap<>();
        data.put("records", pageResult.getRecords());
        data.put("total", pageResult.getTotal());
        data.put("pages", pageResult.getPages());
        data.put("current", pageResult.getCurrent());
        data.put("size", pageResult.getSize());
        return new Result<>(200, data, "查询成功");
    }

    @PostMapping("/videos/upload")
    public Result<List<Videos>> uploadVideos(
            @RequestParam("files") MultipartFile[] files,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (files == null || files.length == 0) {
            return new Result<>(400, null, "请选择文件");
        }
        Long userId = getUserId(authHeader);
        String username = getUsername(authHeader);
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String dirPath = uploadPath + "/videos/" + dateDir;
        File dir = new File(dirPath);
        if (!dir.exists()) dir.mkdirs();

        List<Videos> uploaded = new ArrayList<>();
        for (MultipartFile file : files) {
            try {
                String originalName = file.getOriginalFilename();
                String ext = originalName != null && originalName.contains(".")
                        ? originalName.substring(originalName.lastIndexOf(".")) : "";
                String fileName = DTF.format(LocalDateTime.now()) + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;
                String filePath = dirPath + "/" + fileName;
                file.transferTo(new File(filePath));

                Videos vid = new Videos();
                vid.setFileName(fileName);
                vid.setOriginalName(originalName);
                vid.setFilePath("/uploads/videos/" + dateDir + "/" + fileName);
                vid.setOriginalPath("/uploads/videos/" + dateDir + "/" + fileName);
                vid.setCoverPath("");
                vid.setFileSize(file.getSize());
                vid.setMimeType(file.getContentType());
                vid.setDuration(0L);
                vid.setUserId(userId);
                vid.setUsername(username);
                vid.setStatus(1);
                vid.setCreateTime(LocalDateTime.now());
                videosMapper.insert(vid);
                uploaded.add(vid);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new Result<>(200, uploaded, "上传成功，共" + uploaded.size() + "个文件");
    }

    @DeleteMapping("/videos/{id}")
    public Result<String> deleteVideo(@PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        Videos vid = videosMapper.selectById(id);
        if (vid == null) return new Result<>(404, null, "视频不存在");
        Long userId = getUserId(authHeader);
        if (!isAdmin(authHeader) && (userId == null || !userId.equals(vid.getUserId()))) {
            return new Result<>(403, null, "无权限删除");
        }
        try {
            String fullPath = uploadPath + vid.getFilePath().replace("/uploads", "");
            File f = new File(fullPath);
            if (f.exists()) f.delete();
        } catch (Exception e) { e.printStackTrace(); }
        videosMapper.deleteById(id);
        return new Result<>(200, null, "删除成功");
    }

    // ==================== 工具方法 ====================

    private Long getUserId(String authHeader) {
        try {
            String token = jwtUtil.extractTokenFromHeader(authHeader);
            if (token == null) return null;
            return jwtUtil.getUserIdFromToken(token);
        } catch (Exception e) { return null; }
    }

    private String getUsername(String authHeader) {
        Long userId = getUserId(authHeader);
        if (userId == null) return "unknown";
        try {
            SysUser user = userService.getById(userId);
            return user != null ? user.getUsername() : "unknown";
        } catch (Exception e) { return "unknown"; }
    }

    private boolean isAdmin(String authHeader) {
        Long userId = getUserId(authHeader);
        if (userId == null) return false;
        try {
            SysUser user = userService.getById(userId);
            return user != null && user.getRole() != null && user.getRole() == 1;
        } catch (Exception e) { return false; }
    }
}
