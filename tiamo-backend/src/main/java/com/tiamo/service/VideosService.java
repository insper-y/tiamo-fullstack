package com.tiamo.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tiamo.entity.Videos;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public interface VideosService extends IService<Videos> {
    List<Videos> uploadVideos(MultipartFile[] files, Long userId, String username);
    boolean deleteVideo(Long id);
}
