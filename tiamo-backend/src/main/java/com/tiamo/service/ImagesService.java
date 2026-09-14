package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.Images;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface ImagesService extends IService<Images> {
    List<Images> uploadImages(MultipartFile[] files, Long userId, String username);
    boolean deleteImage(Long id);
}
