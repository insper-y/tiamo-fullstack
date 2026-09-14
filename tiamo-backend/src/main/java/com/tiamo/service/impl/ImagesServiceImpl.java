package com.tiamo.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tiamo.entity.Images;
import com.tiamo.mapper.ImagesMapper;
import com.tiamo.service.ImagesService;
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
public class ImagesServiceImpl extends ServiceImpl<ImagesMapper, Images> implements ImagesService {

    @Value("${upload.path:/tmp/tiamo-uploads}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        baseMapper.createTable();
        File dir = new File(uploadPath + "/images");
        if (!dir.exists()) dir.mkdirs();
    }

    @Override
    public List<Images> uploadImages(MultipartFile[] files, Long userId, String username) {
        List<Images> result = new ArrayList<>();
        String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        File targetDir = new File(uploadPath + "/images/" + dateDir);
        if (!targetDir.exists()) targetDir.mkdirs();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            String originalName = file.getOriginalFilename();
            String ext = originalName != null && originalName.contains(".")
                    ? originalName.substring(originalName.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID().toString().replace("-", "") + ext;
            Path targetPath = Paths.get(targetDir.getAbsolutePath(), fileName);

            try {
                Files.copy(file.getInputStream(), targetPath);
                Images image = new Images();
                image.setFileName(fileName);
                image.setOriginalName(originalName);
                image.setFilePath("/uploads/images/" + dateDir + "/" + fileName);
                image.setFileSize(file.getSize());
                image.setMimeType(file.getContentType());
                image.setUserId(userId);
                image.setUsername(username);
                image.setCreateTime(LocalDateTime.now());
                image.setThumbnailPath("/uploads/images/" + dateDir + "/" + fileName);
                save(image);
                result.add(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public boolean deleteImage(Long id) {
        Images image = getById(id);
        if (image != null) {
            String filePath = uploadPath + image.getFilePath().replace("/uploads", "");
            File file = new File(filePath);
            if (file.exists()) file.delete();
        }
        return removeById(id);
    }
}
