package com.tiamo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.Videos;
import com.tiamo.mapper.VideosMapper;
import com.tiamo.service.VideosService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class VideosServiceImpl extends ServiceImpl<VideosMapper, Videos> implements VideosService {

    @Value("${upload.path:/tmp/tiamo-uploads}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        baseMapper.createTable();
        File dir = new File(uploadPath + "/videos");
        if (!dir.exists()) dir.mkdirs();
    }

    @Override
    public List<Videos> uploadVideos(MultipartFile[] files, Long userId, String username) {
        List<Videos> result = new ArrayList<>();
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        File targetDir = new File(uploadPath + "/videos/" + dateDir);
        if (!targetDir.exists()) targetDir.mkdirs();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".mp4";
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path targetPath = Paths.get(targetDir.getAbsolutePath(), fileName);

            try {
                Files.copy(file.getInputStream(), targetPath);
                Videos video = new Videos();
                video.setFileName(fileName);
                video.setOriginalName(originalName);
                video.setFilePath("/uploads/videos/" + dateDir + "/" + fileName);
                video.setFileSize(file.getSize());
                video.setMimeType(file.getContentType());
                video.setUserId(userId);
                video.setUsername(username);
                video.setCreateTime(LocalDateTime.now());
                video.setStatus(1);
                video.setOriginalPath("/uploads/videos/" + dateDir + "/" + fileName);
                save(video);
                result.add(video);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public boolean deleteVideo(Long id) {
        Videos video = getById(id);
        if (video != null) {
            String filePath = uploadPath + video.getFilePath().replace("/uploads", "");
            File file = new File(filePath);
            if (file.exists()) file.delete();
        }
        return removeById(id);
    }
}
